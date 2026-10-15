package dev.slne.surf.odyssey.runtime.paper.common.skill

import dev.slne.surf.odyssey.core.common.skill.SkillType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SkillSaveSchedulerTest {
    private val uuid = UUID.randomUUID()
    private val xp = ConcurrentHashMap<SkillType, Long>()
    private val saved = mutableListOf<Triple<UUID, SkillType, Long>>()
    private val failures = mutableListOf<Throwable>()
    private var failNext = false

    private fun scheduler(scope: CoroutineScope) = SkillSaveScheduler(
        scope = scope,
        debounce = 5.seconds,
        interval = 300.seconds,
        xpOf = { _, skill -> xp[skill] ?: 0L },
        save = { id, skill, value ->
            if (failNext) {
                failNext = false
                error("microservice down")
            }
            saved.add(Triple(id, skill, value))
        },
        onFailure = { _, _, cause -> failures.add(cause) }
    )

    @Test
    fun `saves after the debounce time without new xp`() = runTest {
        val scheduler = scheduler(backgroundScope)
        xp[SkillType.MINING] = 10

        scheduler.markDirty(uuid, SkillType.MINING)
        advanceTimeBy(4_900); runCurrent()
        assertTrue(saved.isEmpty())

        advanceTimeBy(200); runCurrent()
        assertEquals(listOf(Triple(uuid, SkillType.MINING, 10L)), saved)
    }

    @Test
    fun `new xp restarts the debounce and saves the latest value once`() = runTest {
        val scheduler = scheduler(backgroundScope)

        xp[SkillType.MINING] = 1
        scheduler.markDirty(uuid, SkillType.MINING)
        advanceTimeBy(3_000); runCurrent()

        xp[SkillType.MINING] = 2
        scheduler.markDirty(uuid, SkillType.MINING)
        advanceTimeBy(4_000); runCurrent()
        assertTrue(saved.isEmpty())

        advanceTimeBy(1_100); runCurrent()
        assertEquals(listOf(Triple(uuid, SkillType.MINING, 2L)), saved)
    }

    @Test
    fun `the interval saves even when the debounce never fires`() = runTest {
        val scheduler = scheduler(backgroundScope)
        scheduler.start()

        repeat(299) {
            xp[SkillType.FISHING] = (xp[SkillType.FISHING] ?: 0L) + 1
            scheduler.markDirty(uuid, SkillType.FISHING)
            advanceTimeBy(1_000); runCurrent()
        }
        assertTrue(saved.isEmpty())

        xp[SkillType.FISHING] = 300
        scheduler.markDirty(uuid, SkillType.FISHING)
        advanceTimeBy(1_100); runCurrent()

        assertEquals(listOf(Triple(uuid, SkillType.FISHING, 300L)), saved)
    }

    @Test
    fun `snapshot and persist save immediately for a quitting player`() = runTest {
        val scheduler = scheduler(backgroundScope)
        xp[SkillType.MOB_HUNTING] = 77
        scheduler.markDirty(uuid, SkillType.MOB_HUNTING)

        val snapshot = scheduler.snapshot(uuid)
        xp.clear()
        scheduler.persist(uuid, snapshot)

        assertEquals(listOf(Triple(uuid, SkillType.MOB_HUNTING, 77L)), saved)

        advanceTimeBy(10_000); runCurrent()
        assertEquals(1, saved.size)
    }

    @Test
    fun `a failed save is reported and retried on the next flush`() = runTest {
        val scheduler = scheduler(backgroundScope)
        xp[SkillType.MINING] = 40
        scheduler.markDirty(uuid, SkillType.MINING)

        failNext = true
        scheduler.flush(uuid)
        assertEquals(1, failures.size)
        assertTrue(saved.isEmpty())

        scheduler.flush(uuid)
        assertEquals(listOf(Triple(uuid, SkillType.MINING, 40L)), saved)
    }

    @Test
    fun `flush all saves every dirty player`() = runTest {
        val scheduler = scheduler(backgroundScope)
        val other = UUID.randomUUID()
        xp[SkillType.MINING] = 3

        scheduler.markDirty(uuid, SkillType.MINING)
        scheduler.markDirty(other, SkillType.MINING)
        scheduler.flushAll()

        assertEquals(setOf(uuid, other), saved.map { it.first }.toSet())
    }
}
