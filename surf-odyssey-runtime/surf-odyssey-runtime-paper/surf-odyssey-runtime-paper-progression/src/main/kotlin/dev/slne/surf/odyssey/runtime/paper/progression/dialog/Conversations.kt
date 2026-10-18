package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.AnswerKind
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.NodeAction
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationOutcome
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestAvailability
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestEngine
import dev.slne.surf.odyssey.runtime.paper.progression.state.PlayerRequirementContext
import io.papermc.paper.connection.PlayerGameConnection
import io.papermc.paper.event.player.PlayerCustomClickEvent
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs conversations between players and NPCs.
 *
 * A conversation opens with the greeting of the NPC and continues through the nodes of its quests. Every screen is
 * built from the player's current progression when it is shown. Only the answers of the screen a player currently
 * sees can be clicked; they stay clickable until another screen is shown or the conversation ends. A clicked answer
 * runs only while the player is within [ConversationChecks.MAX_DISTANCE] of the NPC and the quest is still where the
 * screen showed it. Quest actions run through [QuestEngine]; afterwards the conversation goes on as
 * [ConversationActions] decides.
 */
object Conversations {
    /** The answer lines of the screen each player currently sees. */
    private val tokens = AnswerTokens<ConversationLine>()

    /** Whether [register] already ran. */
    private val registered = AtomicBoolean(false)

    /**
     * A conversation with an NPC.
     *
     * @property npc the NPC
     * @property location where the NPC stands
     */
    private class Conversation(val npc: NpcDefinition, val location: Location)

    /**
     * Registers the listener that receives answer clicks. Calling it again has no effect.
     *
     * @param plugin the plugin that owns the listener
     */
    internal fun register(plugin: Plugin) {
        if (!registered.compareAndSet(false, true)) return
        Bukkit.getPluginManager().registerEvents(ClickListener, plugin)
    }

    /**
     * Opens the greeting of [npc] for [player]. Must run on the player's thread. Tells the player that conversations
     * are not available while their progression is not loaded.
     *
     * @param player the player who talks to the NPC
     * @param npc the NPC
     * @param location where the NPC stands; answers are accepted only within [ConversationChecks.MAX_DISTANCE] of it
     */
    fun open(player: Player, npc: NpcDefinition, location: Location) {
        val runtime = Progression.runtimeOrNull
        val progression = runtime?.store?.get(player.uniqueId)
        if (progression == null) {
            ProgressionMessages.unavailable(player)
            return
        }
        val conversation = Conversation(npc, location)
        val now = runtime.clock.instant()
        val context = PlayerRequirementContext(player, progression)
        val offered = QuestAvailability.offeredBy(npc, progression, context, player.uniqueId, now, runtime.registry)
        val active = QuestAvailability.activeAt(npc, progression, now, runtime.registry)
        val lines = GreetingNode.answers(npc, offered, active).map { greetingLine(conversation, it) }
        show(
            player,
            ConversationScreen(
                title = npc.name,
                portrait = portraitOf(npc, NpcDefinition.NEUTRAL_MOOD),
                npcName = npc.name,
                meta = null,
                text = npc.greeting,
                answers = lines,
            )
        )
    }

    /**
     * Returns the line of a greeting answer. When clicked, every answer but leaving checks that the player is still
     * in reach and that the quest is still offered or still continues at the NPC.
     */
    private fun greetingLine(conversation: Conversation, answer: GreetingAnswer): ConversationLine = when (answer) {
        is GreetingAnswer.Offer -> ConversationLine(answer.quest.title, Component.text("Quest ansehen"), AnswerKind.NORMAL) {
            if (inReach(it, conversation) && isOffered(it, answer.quest)) {
                showNode(it, conversation, answer.quest, answer.quest.offerNode, NodePhase.Offered)
            } else {
                refuse(it, ConversationHint.REFUSED)
            }
        }

        is GreetingAnswer.Continue -> ConversationLine(answer.quest.title, Component.text("Quest fortsetzen"), AnswerKind.NORMAL) {
            val step = QuestEngine.progressOf(it, answer.quest)?.step
            if (inReach(it, conversation) && step != null && continuesAt(it, conversation.npc, answer.quest)) {
                showNode(it, conversation, answer.quest, answer.quest.steps[step].entryNode, NodePhase.Step(step))
            } else {
                refuse(it, ConversationHint.REFUSED)
            }
        }

        GreetingAnswer.Trade -> ConversationLine(Component.text("Handeln"), Component.text("Angebote ansehen"), AnswerKind.NORMAL) {
            if (inReach(it, conversation)) openTrade(it, conversation) else refuse(it, ConversationHint.REFUSED)
        }

        GreetingAnswer.Craft -> ConversationLine(Component.text("Herstellen"), Component.text("Station öffnen"), AnswerKind.NORMAL) {
            if (inReach(it, conversation)) openStation(it, conversation) else refuse(it, ConversationHint.REFUSED)
        }

        GreetingAnswer.Leave -> ConversationLine(Component.text("Gespräch beenden"), Component.text("Gespräch beenden"), AnswerKind.LEAVE) {
            close(it)
        }
    }

    /**
     * Shows node [nodeId] of [quest] with the answers whose requirement the player meets. Must run on the player's
     * thread.
     *
     * When an answer is clicked, it runs only if the player is still in reach, its requirement still holds, the quest
     * is still where [phase] says and the action is allowed in [phase]; otherwise the conversation ends with a hint.
     * Ending the conversation is always allowed.
     *
     * @param player the player
     * @param conversation the conversation
     * @param quest the quest owning the node
     * @param nodeId the node
     * @param phase which part of the quest the node belongs to
     */
    private fun showNode(
        player: Player,
        conversation: Conversation,
        quest: QuestDefinition,
        nodeId: String,
        phase: NodePhase,
    ) {
        val runtime = Progression.runtimeOrNull
        val progression = runtime?.store?.get(player.uniqueId)
        if (progression == null) {
            close(player)
            ProgressionMessages.unavailable(player)
            return
        }
        val node = runtime.registry.node(quest, nodeId)
        val context = PlayerRequirementContext(player, progression)
        val lines = node.answers
            .filter { it.visibleWhen?.isMet(context) ?: true }
            .map { answer ->
                ConversationLine(answer.text, answer.tooltip, answer.kind) {
                    if (answer.action == NodeAction.Close) {
                        close(it)
                    } else if (!inReach(it, conversation) || !requirementHolds(it, answer.visibleWhen) ||
                        !ConversationChecks.allows(phase, answer.action) || !phaseHolds(it, quest, phase)
                    ) {
                        refuse(it, ConversationHint.REFUSED)
                    } else {
                        perform(it, conversation, quest, phase, answer.action)
                    }
                }
            }
        show(
            player,
            ConversationScreen(
                title = ConversationRenderer.questTitle(quest.type),
                portrait = portraitOf(node.npc, node.mood),
                npcName = node.npc.name,
                meta = node.meta,
                text = node.text,
                answers = lines,
            )
        )
    }

    /** Carries out [action] picked in a node of [quest] whose phase is [phase]. */
    private suspend fun perform(
        player: Player,
        conversation: Conversation,
        quest: QuestDefinition,
        phase: NodePhase,
        action: NodeAction,
    ) {
        val npc = conversation.npc
        when (action) {
            is NodeAction.Goto -> showNode(player, conversation, quest, action.nodeId, phase)
            NodeAction.Close -> close(player)
            NodeAction.OpenTrade -> openTrade(player, conversation)
            NodeAction.OpenStation -> openStation(player, conversation)
            is NodeAction.AcceptQuest ->
                runQuestAction(player, conversation, quest, action.then) { QuestEngine.accept(player, it) }

            is NodeAction.CompleteTalk ->
                runQuestAction(player, conversation, quest, action.then) { QuestEngine.completeTalk(player, it, npc) }

            is NodeAction.DeliverItems ->
                runQuestAction(player, conversation, quest, action.then) { QuestEngine.deliver(player, it, npc) }

            is NodeAction.TurnIn ->
                runQuestAction(player, conversation, quest, action.then) { QuestEngine.turnIn(player, it, npc) }
        }
    }

    /** Ends the conversation and opens the trade menu of its NPC. */
    private fun openTrade(player: Player, conversation: Conversation) {
        close(player)
        ConversationHooks.openTrade(player, conversation.npc)
    }

    /** Ends the conversation and opens the station of its NPC. */
    private fun openStation(player: Player, conversation: Conversation) {
        close(player)
        ConversationHooks.openStation(player, conversation.npc)
    }

    /**
     * Runs a quest action of [quest] and continues the conversation with its outcome. A node shown afterwards gets the
     * phase the quest is in after the action.
     *
     * @param then the node shown after success, or `null` to end the conversation
     * @param action the quest action
     */
    private suspend fun runQuestAction(
        player: Player,
        conversation: Conversation,
        quest: QuestDefinition,
        then: String?,
        action: suspend (QuestDefinition) -> OperationOutcome,
    ) {
        when (val step = ConversationActions.after(action(quest), then)) {
            is ConversationStep.ShowNode -> showNode(player, conversation, quest, step.nodeId, phaseOf(player, quest))
            ConversationStep.Close -> close(player)
            is ConversationStep.Hint -> refuse(player, step.hint)
            ConversationStep.Ignore -> Unit
        }
    }

    /** Returns whether [player] stands in the world of the conversation's NPC and within reach of it. */
    private fun inReach(player: Player, conversation: Conversation): Boolean =
        player.world == conversation.location.world &&
                ConversationChecks.inReach(player.location.distanceSquared(conversation.location))

    /** Returns whether [requirement] holds for [player] now; `null` always holds. */
    private fun requirementHolds(player: Player, requirement: Requirement?): Boolean {
        if (requirement == null) return true
        val progression = Progression.runtimeOrNull?.store?.get(player.uniqueId) ?: return false
        return requirement.isMet(PlayerRequirementContext(player, progression))
    }

    /** Returns whether [quest] is offered to [player] now. */
    private fun isOffered(player: Player, quest: QuestDefinition): Boolean {
        val runtime = Progression.runtimeOrNull ?: return false
        val progression = runtime.store[player.uniqueId] ?: return false
        val context = PlayerRequirementContext(player, progression)
        val now = runtime.clock.instant()
        return QuestAvailability.isOffered(quest, progression, context, player.uniqueId, now, runtime.registry)
    }

    /** Returns whether the active [quest] of [player] continues at [npc] now. */
    private fun continuesAt(player: Player, npc: NpcDefinition, quest: QuestDefinition): Boolean {
        val runtime = Progression.runtimeOrNull ?: return false
        val progression = runtime.store[player.uniqueId] ?: return false
        return QuestAvailability.activeAt(npc, progression, runtime.clock.instant(), runtime.registry)
            .any { it.id == quest.id }
    }

    /** Returns whether [quest] is still where [phase] says for [player]. */
    private fun phaseHolds(player: Player, quest: QuestDefinition, phase: NodePhase): Boolean {
        val offered = phase == NodePhase.Offered && isOffered(player, quest)
        return ConversationChecks.phaseHolds(phase, offered, QuestEngine.progressOf(player, quest)?.step)
    }

    /** Returns the phase [quest] is in for [player] now: its current step, offered, or done. */
    private fun phaseOf(player: Player, quest: QuestDefinition): NodePhase {
        QuestEngine.progressOf(player, quest)?.let { return NodePhase.Step(it.step) }
        return if (isOffered(player, quest)) NodePhase.Offered else NodePhase.Done
    }

    /** Returns the portrait of [npc] in [mood], or no portrait if looking it up fails. */
    private fun portraitOf(npc: NpcDefinition, mood: String): Component = try {
        Portraits.of(npc, mood)
    } catch (_: LinkageError) {
        Component.empty()
    } catch (_: RuntimeException) {
        Component.empty()
    }

    /** Ends the conversation of [player] and tells them [hint]. */
    private fun refuse(player: Player, hint: ConversationHint) {
        close(player)
        player.sendText {
            appendErrorPrefix()
            error(hint.text)
        }
    }

    /** Shows [screen] to [player] and makes its answers the only clickable ones. */
    private fun show(player: Player, screen: ConversationScreen) {
        val token = tokens.show(player.uniqueId, screen.answers)
        player.showDialog(ConversationRenderer.render(screen, token))
    }

    /** Ends the conversation of [player]: closes the dialog and makes its answers stale. */
    private fun close(player: Player) {
        tokens.close(player.uniqueId)
        player.closeDialog()
    }

    /**
     * Handles a click on line [index] of the screen with [token] on the player's thread. Stale tokens and unknown
     * lines are ignored; [AnswerPayload.CLOSE] ends the conversation.
     */
    private fun onAnswer(player: Player, token: UUID, index: Int) {
        val plugin = ProgressionModule.plugin
        plugin.launch(plugin.entityDispatcher(player)) {
            if (!player.isOnline) return@launch
            if (index == AnswerPayload.CLOSE) {
                tokens.closeIfCurrent(player.uniqueId, token)
                return@launch
            }
            tokens.resolve(player.uniqueId, token, index)?.onClick?.invoke(player)
        }
    }

    /** Receives answer clicks and forgets the screens of leaving players. */
    private object ClickListener : Listener {
        /** Passes clicks with the identifier [AnswerPayload.KEY] from players in the game to [onAnswer]. */
        @EventHandler
        fun onCustomClick(event: PlayerCustomClickEvent) {
            if (event.identifier != AnswerPayload.KEY) return
            val player = (event.commonConnection as? PlayerGameConnection)?.player ?: return
            val (token, index) = AnswerPayload.decode(event.tag?.string()) ?: return
            onAnswer(player, token, index)
        }

        /** Forgets the screen of the leaving player. */
        @EventHandler(priority = EventPriority.MONITOR)
        fun onQuit(event: PlayerQuitEvent) {
            tokens.close(event.player.uniqueId)
        }
    }
}
