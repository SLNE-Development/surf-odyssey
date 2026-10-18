package dev.slne.surf.odyssey.core.common.progression

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import kotlinx.serialization.Serializable

/**
 * The stored state of one quest for one player.
 *
 * @property questId the id of the quest definition
 * @property status where the quest stands in its lifecycle
 * @property step the index of the step the player is currently on
 * @property stepVersion the version of [step] the player's progress was recorded against
 * @property period the period index the quest was accepted in; `0` for quests that do not recur
 * @property completedAt the epoch milliseconds the quest was completed at, or `null` while not completed
 */
@Serializable
data class QuestStateDto(
    val questId: String,
    val status: QuestStatus,
    val step: Int,
    val stepVersion: Int,
    val period: Long,
    val completedAt: Long?
)

/**
 * The stored value of one objective counter of a quest step.
 *
 * @property questId the id of the quest definition
 * @property step the index of the step the objective belongs to
 * @property stepVersion the version of the step the counter was recorded against
 * @property objective the index of the objective within the step
 * @property period the period index the counter belongs to
 * @property value the current count
 */
@Serializable
data class CounterDto(
    val questId: String,
    val step: Int,
    val stepVersion: Int,
    val objective: Int,
    val period: Long,
    val value: Int
)

/**
 * An amount to add to one objective counter of a quest step.
 *
 * @property questId the id of the quest definition
 * @property step the index of the step the objective belongs to
 * @property stepVersion the version of the step the delta was counted against
 * @property objective the index of the objective within the step
 * @property period the period index the delta belongs to
 * @property delta the amount to add to the counter
 */
@Serializable
data class CounterDeltaDto(
    val questId: String,
    val step: Int,
    val stepVersion: Int,
    val objective: Int,
    val period: Long,
    val delta: Int
)

/**
 * An item waiting in a player's pickup box.
 *
 * @property id the id of the delivery, used to claim it
 * @property itemBase64 the item serialized as bytes and encoded as Base64
 * @property source a short identifier of what created the delivery
 * @property createdAt the epoch milliseconds the delivery was created at
 */
@Serializable
data class DeliveryDto(
    val id: SerializableUUID,
    val itemBase64: String,
    val source: String,
    val createdAt: Long
)

/**
 * An item an operation puts into the player's pickup box.
 *
 * @property itemBase64 the item serialized as bytes and encoded as Base64
 * @property source a short identifier of what creates the delivery
 */
@Serializable
data class NewDeliveryDto(
    val itemBase64: String,
    val source: String
)

/**
 * A timed crafting job at a station that has not been collected yet.
 *
 * @property id the id of the job
 * @property stationType the station type the job runs at and can be collected from
 * @property recipeId the id of the recipe being crafted
 * @property amount how many times the recipe is crafted
 * @property startedAt the epoch milliseconds the job started at
 * @property readyAt the epoch milliseconds from which the job can be collected
 */
@Serializable
data class StationJobDto(
    val id: SerializableUUID,
    val stationType: String,
    val recipeId: String,
    val amount: Int,
    val startedAt: Long,
    val readyAt: Long
)

/**
 * Everything stored about one player's progression.
 *
 * @property player the player the state belongs to
 * @property quests the state of every quest the player has accepted
 * @property counters every stored objective counter
 * @property recipes the ids of the unlocked recipes
 * @property titles the ids of the unlocked titles
 * @property activeTitle the id of the title the player shows, or `null` for none
 * @property trackedQuest the id of the quest the player tracks, or `null` for none
 * @property deliveries the unclaimed deliveries in the pickup box
 * @property jobs the station jobs that have not been collected yet
 */
@Serializable
data class ProgressionStateDto(
    val player: SerializableUUID,
    val quests: List<QuestStateDto>,
    val counters: List<CounterDto>,
    val recipes: Set<String>,
    val titles: Set<String>,
    val activeTitle: String?,
    val trackedQuest: String?,
    val deliveries: List<DeliveryDto>,
    val jobs: List<StationJobDto>
)

/** One change to a player's progression that an operation applies. */
@Serializable
sealed interface StateChange {
    /**
     * Starts a quest at its first step, replacing an abandoned state or a state of an earlier period.
     *
     * @property questId the id of the quest definition
     * @property stepVersion the version of the first step
     * @property period the period index the quest is accepted in
     */
    @Serializable
    data class QuestAccepted(val questId: String, val stepVersion: Int, val period: Long) : StateChange

    /**
     * Moves an active quest from [fromStep] to [toStep].
     *
     * @property questId the id of the quest definition
     * @property fromStep the step the quest must currently be on
     * @property toStep the step the quest moves to
     * @property stepVersion the version of [toStep]
     */
    @Serializable
    data class QuestStepAdvanced(val questId: String, val fromStep: Int, val toStep: Int, val stepVersion: Int) :
        StateChange

    /**
     * Completes an active quest that is on [fromStep].
     *
     * @property questId the id of the quest definition
     * @property fromStep the step the quest must currently be on
     */
    @Serializable
    data class QuestCompleted(val questId: String, val fromStep: Int) : StateChange

    /**
     * Sets one objective counter to an absolute value.
     *
     * @property questId the id of the quest definition
     * @property step the index of the step the objective belongs to
     * @property stepVersion the version of the step
     * @property objective the index of the objective within the step
     * @property period the period index the counter belongs to
     * @property value the new count
     */
    @Serializable
    data class CounterSet(
        val questId: String,
        val step: Int,
        val stepVersion: Int,
        val objective: Int,
        val period: Long,
        val value: Int
    ) : StateChange

    /**
     * Unlocks a recipe; unlocking a known recipe changes nothing.
     *
     * @property recipeId the id of the recipe
     */
    @Serializable
    data class RecipeUnlocked(val recipeId: String) : StateChange

    /**
     * Unlocks a title; unlocking a known title changes nothing.
     *
     * @property titleId the id of the title
     */
    @Serializable
    data class TitleUnlocked(val titleId: String) : StateChange

    /**
     * Starts a timed station job.
     *
     * @property job the job to store
     */
    @Serializable
    data class JobStarted(val job: StationJobDto) : StateChange

    /**
     * Marks an uncollected station job as collected.
     *
     * @property jobId the id of the job
     */
    @Serializable
    data class JobCollected(val jobId: SerializableUUID) : StateChange
}

/**
 * A set of changes and deliveries that is stored completely or not at all.
 *
 * @property id the id that makes repeated commits of the same operation a no-op
 * @property player the player the operation belongs to
 * @property changes the changes to apply, checked in order
 * @property deliveries the items to put into the player's pickup box
 */
@Serializable
data class OperationDto(
    val id: SerializableUUID,
    val player: SerializableUUID,
    val changes: List<StateChange>,
    val deliveries: List<NewDeliveryDto>
)

/** The outcome of committing an operation. */
@Serializable
sealed interface OperationResultDto {
    /**
     * The operation was stored by this commit.
     *
     * @property deliveryIds the ids of the created deliveries, in the order of the operation's deliveries
     */
    @Serializable
    data class Applied(val deliveryIds: List<SerializableUUID>) : OperationResultDto

    /**
     * The operation had already been stored by an earlier commit with the same id.
     *
     * @property deliveryIds the ids of the deliveries the earlier commit created
     */
    @Serializable
    data class AlreadyApplied(val deliveryIds: List<SerializableUUID>) : OperationResultDto

    /**
     * Nothing of the operation was stored.
     *
     * @property reason a short description of the change that could not be applied
     */
    @Serializable
    data class Rejected(val reason: String) : OperationResultDto
}

/**
 * The registration of one progression tool item.
 *
 * @property tool the unique id stored on the item
 * @property toolId the id of the tool definition
 * @property creator the player the tool was created for
 * @property lastHolder the player the tool was last seen with, or `null` if it was never seen
 * @property lastSeenAt the epoch milliseconds the tool was last seen at, or `null` if it was never seen
 */
@Serializable
data class ToolRecordDto(
    val tool: SerializableUUID,
    val toolId: String,
    val creator: SerializableUUID,
    val lastHolder: SerializableUUID?,
    val lastSeenAt: Long?
)

/** The outcome of reporting that a player holds a tool. */
@Serializable
enum class ToolSightingResult {
    /** The sighting is unremarkable. */
    OK,

    /** No tool with the reported id is registered. */
    UNKNOWN_TOOL,

    /** The tool changed hands back and forth within a short time, which suggests a duplicated item. */
    DUPLICATE_SUSPECTED
}
