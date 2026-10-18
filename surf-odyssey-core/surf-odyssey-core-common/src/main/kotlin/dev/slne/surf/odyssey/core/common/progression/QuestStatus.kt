package dev.slne.surf.odyssey.core.common.progression

import kotlinx.serialization.Serializable

/** Lifecycle state of a quest for one player. */
@Serializable
enum class QuestStatus { ACTIVE, COMPLETED, ABANDONED }
