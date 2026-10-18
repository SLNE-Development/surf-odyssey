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
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/**
 * Runs conversations between players and NPCs.
 *
 * A conversation opens with the greeting of the NPC and continues through the nodes of its quests. Every screen is
 * built from the player's current progression when it is shown. Quest actions run through [QuestEngine]; afterwards
 * the conversation goes on as [ConversationActions] decides.
 */
object Conversations {
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
        player.showDialog(
            ConversationRenderer.render(
                ConversationScreen(
                    title = npc.name,
                    portrait = Portraits.of(npc, NpcDefinition.NEUTRAL_MOOD),
                    npcName = npc.name,
                    meta = null,
                    text = npc.greeting,
                    answers = lines,
                )
            )
        )
    }

    /** Returns the line of a greeting answer of [npc]. */
    private fun greetingLine(npc: NpcDefinition, answer: GreetingAnswer): ConversationLine = when (answer) {
        is GreetingAnswer.Offer -> ConversationLine(answer.quest.title, Component.text("Quest ansehen"), AnswerKind.NORMAL) {
            onPlayerThread(it) { showNode(it, npc, answer.quest, answer.quest.offerNode) }
        }

        is GreetingAnswer.Continue -> ConversationLine(answer.quest.title, Component.text("Quest fortsetzen"), AnswerKind.NORMAL) {
            onPlayerThread(it) { continueQuest(it, npc, answer.quest) }
        }

        GreetingAnswer.Trade -> ConversationLine(Component.text("Handeln"), Component.text("Angebote ansehen"), AnswerKind.NORMAL) {
            onPlayerThread(it) { perform(it, npc, null, NodeAction.OpenTrade) }
        }

        GreetingAnswer.Craft -> ConversationLine(Component.text("Herstellen"), Component.text("Station öffnen"), AnswerKind.NORMAL) {
            onPlayerThread(it) { perform(it, npc, null, NodeAction.OpenStation) }
        }

        GreetingAnswer.Leave -> ConversationLine(Component.text("Gespräch beenden"), Component.text("Gespräch beenden"), AnswerKind.LEAVE) {
            onPlayerThread(it) { it.closeDialog() }
        }
    }

    /** Shows the entry node of the current step of [quest], or closes the conversation if the quest is not active. */
    private fun continueQuest(player: Player, npc: NpcDefinition, quest: QuestDefinition) {
        val step = QuestEngine.progressOf(player, quest)?.step
        if (step == null) {
            player.closeDialog()
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
            player.closeDialog()
            ProgressionMessages.unavailable(player)
            return
        }
        val node = runtime.registry.node(quest, nodeId)
        val context = PlayerRequirementContext(player, progression)
        val lines = node.answers
            .filter { it.visibleWhen?.isMet(context) ?: true }
            .map { answer ->
                ConversationLine(answer.text, answer.tooltip, answer.kind) {
                    onPlayerThread(it) { perform(it, npc, quest, answer.action) }
                }
            }
        player.showDialog(
            ConversationRenderer.render(
                ConversationScreen(
                    title = ConversationRenderer.questTitle(quest.type),
                    portrait = Portraits.of(node.npc, node.mood),
                    npcName = node.npc.name,
                    meta = node.meta,
                    text = node.text,
                    answers = lines,
                )
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
            is NodeAction.Goto -> quest?.let { showNode(player, npc, it, action.nodeId) } ?: player.closeDialog()
            NodeAction.Close -> player.closeDialog()
            NodeAction.OpenTrade -> {
                player.closeDialog()
                ConversationHooks.openTrade(player, npc)
            }

            NodeAction.OpenStation -> {
                player.closeDialog()
                ConversationHooks.openStation(player, npc)
            }

            is NodeAction.AcceptQuest -> runQuestAction(player, npc, quest, action.then) { QuestEngine.accept(player, it) }
            is NodeAction.CompleteTalk -> runQuestAction(player, npc, quest, action.then) { QuestEngine.completeTalk(player, it) }
            is NodeAction.DeliverItems -> runQuestAction(player, npc, quest, action.then) { QuestEngine.deliver(player, it, npc) }
            is NodeAction.TurnIn -> runQuestAction(player, npc, quest, action.then) { QuestEngine.turnIn(player, it) }
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
            player.closeDialog()
            return
        }
        when (val step = ConversationActions.after(action(quest), then)) {
            is ConversationStep.ShowNode -> showNode(player, npc, quest, step.nodeId)
            ConversationStep.Close -> player.closeDialog()
            is ConversationStep.Hint -> {
                player.closeDialog()
                player.sendText {
                    appendErrorPrefix()
                    error(step.hint.text)
                }
            }

            ConversationStep.Ignore -> Unit
        }
    }

    /** Runs [block] for [player] on the player's thread, if the player is still online. */
    private fun onPlayerThread(player: Player, block: suspend (Player) -> Unit) {
        val plugin = ProgressionModule.plugin
        plugin.launch(plugin.entityDispatcher(player)) {
            if (player.isOnline) block(player)
        }
    }
}
