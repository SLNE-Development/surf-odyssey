package dev.slne.surf.odyssey.runtime.paper.common.skill

import dev.slne.surf.odyssey.core.common.skill.SkillType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

class SkillSaveScheduler(
    private val scope: CoroutineScope,
    private val debounce: Duration,
    private val interval: Duration,
    private val xpOf: (UUID, SkillType) -> Long,
    private val save: suspend (UUID, SkillType, Long) -> Unit,
    private val onFailure: (UUID, SkillType, Throwable) -> Unit
) {
    private val dirty = ConcurrentHashMap<UUID, MutableSet<SkillType>>()
    private val debounceJobs = ConcurrentHashMap<UUID, Job>()
    private var intervalJob: Job? = null

    fun start() {
        intervalJob = scope.launch {
            while (isActive) {
                delay(interval)
                flushAll()
            }
        }
    }

    fun stop() {
        intervalJob?.cancel()
        debounceJobs.values.forEach { it.cancel() }
        debounceJobs.clear()
    }

    fun markDirty(uuid: UUID, skill: SkillType) {
        dirty.computeIfAbsent(uuid) { ConcurrentHashMap.newKeySet() }.add(skill)

        debounceJobs.compute(uuid) { _, old ->
            old?.cancel()
            scope.launch {
                delay(debounce)
                flush(uuid)
            }
        }
    }

    fun snapshot(uuid: UUID): Map<SkillType, Long> {
        val skills = dirty.remove(uuid) ?: return emptyMap()
        return skills.associateWith { xpOf(uuid, it) }
    }

    suspend fun persist(uuid: UUID, snapshot: Map<SkillType, Long>) {
        withContext(NonCancellable) {
            snapshot.forEach { (skill, xp) ->
                try {
                    save(uuid, skill, xp)
                } catch (e: Exception) {
                    dirty.computeIfAbsent(uuid) { ConcurrentHashMap.newKeySet() }.add(skill)
                    onFailure(uuid, skill, e)
                }
            }
        }
    }

    suspend fun flush(uuid: UUID) {
        persist(uuid, snapshot(uuid))
    }

    suspend fun flushAll() {
        dirty.keys.toList().forEach { flush(it) }
    }

    fun forget(uuid: UUID) {
        debounceJobs.remove(uuid)?.cancel()
    }
}
