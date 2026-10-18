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
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationOutcome
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestAvailability
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestEngine
import dev.slne.surf.odyssey.runtime.paper.progression.state.PlayerRequirementContext
import io.papermc.paper.connection.PlayerGameConnection
import io.papermc.paper.event.player.PlayerCustomClickEvent
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
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
 * sees can be clicked; they stay clickable until another screen is shown or the conversation ends. Quest actions run
 * through [QuestEngine]; afterwards the conversation goes on as [ConversationActions] decides.
 */
object Conversations {
    /** The answer lines of the screen each player currently sees. */
    private val tokens = AnswerTokens<ConversationLine>()

    /** Whether [register] already ran. */
    private val registered = AtomicBoolean(false)

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
     */
    fun open(player: Player, npc: NpcDefinition) {
        val runtime = Progression.runtimeOrNull
        val progression = runtime?.store?.get(player.uniqueId)
        if (progression == null) {
            ProgressionMessages.unavailable(player)
            return
        }
        val now = runtime.clock.instant()
        val context = PlayerRequirementContext(player, progression)
        val offered = QuestAvailability.offeredBy(npc, progression, context, player.uniqueId, now, runtime.registry)
        val active = QuestAvailability.activeAt(npc, progression, now, runtime.registry)
        val lines = GreetingNode.answers(npc, offered, active).map { greetingLine(npc, it) }
        show(
            player,
            ConversationScreen(
                title = npc.name,
                portrait = Portraits.of(npc, NpcDefinition.NEUTRAL_MOOD),
                npcName = npc.name,
                meta = null,
                text = npc.greeting,
                answers = lines,
            )
        )
    }

    /** Returns the line of a greeting answer of [npc]. */
    private fun greetingLine(npc: NpcDefinition, answer: GreetingAnswer): ConversationLine = when (answer) {
        is GreetingAnswer.Offer -> ConversationLine(answer.quest.title, Component.text("Quest ansehen"), AnswerKind.NORMAL) {
            showNode(it, npc, answer.quest, answer.quest.offerNode)
        }

        is GreetingAnswer.Continue -> ConversationLine(answer.quest.title, Component.text("Quest fortsetzen"), AnswerKind.NORMAL) {
            continueQuest(it, npc, answer.quest)
        }

        GreetingAnswer.Trade -> ConversationLine(Component.text("Handeln"), Component.text("Angebote ansehen"), AnswerKind.NORMAL) {
            perform(it, npc, null, NodeAction.OpenTrade)
        }

        GreetingAnswer.Craft -> ConversationLine(Component.text("Herstellen"), Component.text("Station öffnen"), AnswerKind.NORMAL) {
            perform(it, npc, null, NodeAction.OpenStation)
        }

        GreetingAnswer.Leave -> ConversationLine(Component.text("Gespräch beenden"), Component.text("Gespräch beenden"), AnswerKind.LEAVE) {
            close(it)
        }
    }

    /** Shows the entry node of the current step of [quest], or closes the conversation if the quest is not active. */
    private fun continueQuest(player: Player, npc: NpcDefinition, quest: QuestDefinition) {
        val step = QuestEngine.progressOf(player, quest)?.step
        if (step == null) {
            close(player)
            return
        }
        showNode(player, npc, quest, quest.steps[step].entryNode)
    }

    /**
     * Shows node [nodeId] of [quest] with the answers whose requirement the player meets. Must run on the player's
     * thread.
     *
     * @param player the player
     * @param npc the NPC the conversation is held with
     * @param quest the quest owning the node
     * @param nodeId the node
     */
    private fun showNode(player: Player, npc: NpcDefinition, quest: QuestDefinition, nodeId: String) {
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
                ConversationLine(answer.text, answer.tooltip, answer.kind) { perform(it, npc, quest, answer.action) }
            }
        show(
            player,
            ConversationScreen(
                title = ConversationRenderer.questTitle(quest.type),
                portrait = Portraits.of(node.npc, node.mood),
                npcName = node.npc.name,
                meta = node.meta,
                text = node.text,
                answers = lines,
            )
        )
    }

    /**
     * Carries out [action] picked in a conversation with [npc] about [quest].
     *
     * @param quest the quest of the node the action was picked in, or `null` in the greeting
     */
    private suspend fun perform(player: Player, npc: NpcDefinition, quest: QuestDefinition?, action: NodeAction) {
        when (action) {
            is NodeAction.Goto -> quest?.let { showNode(player, npc, it, action.nodeId) } ?: close(player)
            NodeAction.Close -> close(player)
            NodeAction.OpenTrade -> {
                close(player)
                ConversationHooks.openTrade(player, npc)
            }

            NodeAction.OpenStation -> {
                close(player)
                ConversationHooks.openStation(player, npc)
            }

            is NodeAction.AcceptQuest -> runQuestAction(player, npc, quest, action.then) { QuestEngine.accept(player, it) }
            is NodeAction.CompleteTalk -> runQuestAction(player, npc, quest, action.then) { QuestEngine.completeTalk(player, it, npc) }
            is NodeAction.DeliverItems -> runQuestAction(player, npc, quest, action.then) { QuestEngine.deliver(player, it, npc) }
            is NodeAction.TurnIn -> runQuestAction(player, npc, quest, action.then) { QuestEngine.turnIn(player, it, npc) }
        }
    }

    /**
     * Runs a quest action of [quest] and continues the conversation with its outcome. Closes the conversation when
     * there is no quest.
     *
     * @param then the node shown after success, or `null` to end the conversation
     * @param action the quest action
     */
    private suspend fun runQuestAction(
        player: Player,
        npc: NpcDefinition,
        quest: QuestDefinition?,
        then: String?,
        action: suspend (QuestDefinition) -> OperationOutcome,
    ) {
        if (quest == null) {
            close(player)
            return
        }
        when (val step = ConversationActions.after(action(quest), then)) {
            is ConversationStep.ShowNode -> showNode(player, npc, quest, step.nodeId)
            ConversationStep.Close -> close(player)
            is ConversationStep.Hint -> refuse(player, step.hint)
            ConversationStep.Ignore -> Unit
        }
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
