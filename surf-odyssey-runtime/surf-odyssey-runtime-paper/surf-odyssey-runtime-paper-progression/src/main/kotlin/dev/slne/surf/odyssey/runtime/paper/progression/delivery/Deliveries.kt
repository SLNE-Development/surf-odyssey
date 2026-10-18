package dev.slne.surf.odyssey.runtime.paper.progression.delivery

import dev.slne.surf.odyssey.core.common.progression.NewDeliveryDto
import dev.slne.surf.odyssey.core.common.progression.OperationDto
import dev.slne.surf.odyssey.core.common.progression.OperationResultDto
import dev.slne.surf.odyssey.runtime.paper.progression.StackCodec
import dev.slne.surf.odyssey.runtime.paper.progression.operation.EscrowInventory
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationBackend
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationHooks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.time.Duration

/** The result of claiming one delivery from the pickup box. */
enum class ClaimResult {
    /** The delivery was claimed and its item given to the player. */
    CLAIMED,

    /** The delivery was already claimed, possibly on another server. */
    GONE,

    /** The progression service could not be reached; the delivery stays in the pickup box. */
    FAILED,

    /** The delivery was claimed, but its item could not be decoded and is lost. */
    BROKEN
}

/**
 * Claims deliveries and puts their items into inventories, and puts items into the pickup box.
 *
 * @param backend the progression service
 * @param codec encodes and decodes the items of deliveries
 * @param hooks the server the deliveries are handled on
 * @param commitTimeout how long a commit may take before it counts as failed
 */
class Deliveries(
    private val backend: OperationBackend,
    private val codec: StackCodec,
    private val hooks: OperationHooks,
    private val commitTimeout: Duration,
) {
    /**
     * Commits [operation], reporting failures and timeouts to [OperationHooks.onServiceFailure].
     *
     * @return the outcome, or `null` if the commit failed or timed out
     */
    suspend fun commit(operation: OperationDto): OperationResultDto? = try {
        withTimeout(commitTimeout) { backend.commit(operation) }
    } catch (e: TimeoutCancellationException) {
        hooks.onServiceFailure("commit operation ${operation.id}", e)
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        hooks.onServiceFailure("commit operation ${operation.id}", e)
        null
    }

    /**
     * Commits [operation] and, if the first attempt fails or times out, commits the same operation once more. The
     * second attempt runs even while the service is marked unavailable; a repeated operation id is stored at most
     * once, so the retry learns whether the first attempt was stored.
     *
     * @return the outcome, or `null` if both attempts failed or timed out
     */
    suspend fun commitWithRetry(operation: OperationDto): OperationResultDto? = commit(operation) ?: commit(operation)

    /**
     * Claims each delivery and gives its item to [inventory]. Items that do not fit are put back into the pickup box;
     * deliveries that cannot be claimed because the service fails stay there. A claimed item that cannot be decoded
     * is reported and skipped. The player is told once if anything
     * is left in the pickup box.
     *
     * @param player the owner of the deliveries
     * @param inventory the inventory the items go to
     * @param ids the deliveries to claim
     * @param source the source stored with items put back into the pickup box
     */
    suspend fun deliver(player: UUID, inventory: EscrowInventory, ids: List<UUID>, source: String) {
        var leftInBox = false
        val overflow = mutableListOf<ItemStack>()
        for (id in ids) {
            val encoded = try {
                backend.claim(player, id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                hooks.onServiceFailure("claim delivery $id", e)
                leftInBox = true
                break
            } ?: continue
            val stack = decode(player, id, encoded) ?: continue
            overflow += inventory.give(listOf(stack))
        }
        if (overflow.isNotEmpty() && stash(player, overflow, source)) leftInBox = true
        if (leftInBox) hooks.onLeftInPickupBox(player)
    }

    /**
     * Claims one delivery from the pickup box and gives its item to [inventory]. If the item does not fit after
     * all, it is put back into the pickup box.
     *
     * @param player the owner of the delivery
     * @param inventory the inventory the item goes to
     * @param deliveryId the delivery to claim
     * @param source the source stored if the item is put back into the pickup box
     */
    suspend fun claim(player: UUID, inventory: EscrowInventory, deliveryId: UUID, source: String): ClaimResult {
        val encoded = try {
            backend.claim(player, deliveryId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            hooks.onServiceFailure("claim delivery $deliveryId", e)
            return ClaimResult.FAILED
        } ?: return ClaimResult.GONE
        val stack = decode(player, deliveryId, encoded) ?: return ClaimResult.BROKEN
        val overflow = inventory.give(listOf(stack))
        if (overflow.isNotEmpty() && stash(player, overflow, source)) hooks.onLeftInPickupBox(player)
        return ClaimResult.CLAIMED
    }

    /**
     * Decodes the item of a claimed delivery, reporting a failure to [OperationHooks.onBrokenDelivery].
     *
     * @return the item, or `null` if it cannot be decoded
     */
    private fun decode(player: UUID, deliveryId: UUID, encoded: String): ItemStack? = try {
        codec.decode(encoded)
    } catch (e: Exception) {
        hooks.onBrokenDelivery(player, deliveryId, e)
        null
    }

    /**
     * Puts [items] into the pickup box of [player] with an operation of their own. If that fails, the items are
     * dropped at the player's feet.
     *
     * @param source the source stored with the deliveries
     * @return `true` if the items are in the pickup box, `false` if they were dropped
     */
    suspend fun stash(player: UUID, items: List<ItemStack>, source: String): Boolean {
        val operation = OperationDto(
            UUID.randomUUID(), player, emptyList(), items.map { NewDeliveryDto(codec.encode(it), source) }
        )
        val stored = when (commitWithRetry(operation)) {
            is OperationResultDto.Applied, is OperationResultDto.AlreadyApplied -> true
            is OperationResultDto.Rejected, null -> false
        }
        if (!stored) hooks.dropAtFeet(player, items)
        return stored
    }
}
