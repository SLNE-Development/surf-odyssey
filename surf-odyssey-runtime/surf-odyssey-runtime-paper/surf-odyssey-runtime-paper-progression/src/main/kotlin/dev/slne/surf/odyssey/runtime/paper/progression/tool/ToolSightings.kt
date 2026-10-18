package dev.slne.surf.odyssey.runtime.paper.progression.tool

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.odyssey.core.common.progression.ProgressionBridge
import dev.slne.surf.odyssey.core.common.progression.ToolSightingResult
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionPermissions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Reports tool sightings to the progression service and reacts to the answer: a suspected duplicate is logged as a
 * warning and told to the online admins, an unknown tool is logged once per tool, and nothing happens in game.
 *
 * @param report sends one sighting (tool, holder, server) to the progression service
 * @param serverName returns the name of this server
 * @param logger the log warnings are written to
 * @param notifyAdmins sends a message to every online admin
 * @param onFailure called when a report fails
 */
class SightingReporter(
    private val report: suspend (UUID, UUID, String) -> ToolSightingResult,
    private val serverName: () -> String,
    private val logger: Logger,
    private val notifyAdmins: (Component) -> Unit,
    private val onFailure: (Throwable) -> Unit,
) {
    /** The tools already logged as unknown. */
    private val unknown: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    /**
     * Reports that the player [holderId] named [holderName] holds the tool item [tool] of the tool definition
     * [toolId] and reacts to the answer.
     */
    suspend fun report(tool: UUID, toolId: String, holderId: UUID, holderName: String) {
        val server = serverName()
        val result = try {
            report(tool, holderId, server)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onFailure(e)
            return
        }
        when (result) {
            ToolSightingResult.OK -> Unit
            ToolSightingResult.UNKNOWN_TOOL -> if (unknown.add(tool)) {
                logger.warning("Tool $tool ($toolId) held by $holderName ($holderId) on $server is not registered")
            }
            ToolSightingResult.DUPLICATE_SUSPECTED -> {
                logger.warning("Tool $tool ($toolId) is suspected to be duplicated: held by $holderName ($holderId) on $server")
                notifyAdmins(buildText {
                    appendWarningPrefix()
                    warning("Werkzeug ")
                    variableValue(tool.toString())
                    warning(" ($toolId) ist vermutlich dupliziert: gesehen bei ")
                    variableValue(holderName)
                    warning(" auf ")
                    variableValue(server)
                    warning(".")
                })
            }
        }
    }
}

/**
 * Reports which players hold which tool items: every [interval] for the inventories of all online players, and
 * immediately when a player takes a tool over.
 *
 * @param plugin the plugin that owns the coroutines and the log
 * @param serverName returns the name of this server, as sent with every sighting
 * @param interval the time between two inventory scans
 * @param bridge the progression service
 */
class ToolSightings(
    private val plugin: Plugin,
    serverName: () -> String,
    private val interval: Duration = 60.seconds,
    bridge: ProgressionBridge = ProgressionBridge,
) {
    /** Sends the sightings and reacts to the answers. */
    private val reporter = SightingReporter(
        report = bridge::reportToolSighting,
        serverName = serverName,
        logger = plugin.logger,
        notifyAdmins = { message ->
            plugin.server.onlinePlayers.filter { it.hasPermission(ProgressionPermissions.ADMIN) }
                .forEach { it.sendMessage(message) }
        },
        onFailure = { Progression.reportFailure(plugin.logger, "report a tool sighting", it) },
    )

    /** Starts scanning the inventories of all online players every [interval] until the plugin is disabled. */
    fun start() {
        plugin.launch {
            while (isActive) {
                delay(interval)
                plugin.server.onlinePlayers.toList().forEach { player -> scan(player) }
            }
        }
    }

    /**
     * Reports in the background that [player] holds the tool item with [state].
     *
     * @param player the holder
     * @param state the state of the tool item
     */
    fun reportNow(player: Player, state: ToolState) {
        plugin.launch { reporter.report(state.uuid, state.toolId, player.uniqueId, player.name) }
    }

    /** Reads the tool items of [player] on the player's thread and reports each of them. */
    private suspend fun scan(player: Player) {
        if (!player.isOnline) return
        val tools = withContext(plugin.entityDispatcher(player)) {
            player.inventory.contents.mapNotNull(ToolItems::read).distinctBy { it.uuid }
        }
        tools.forEach { reporter.report(it.uuid, it.toolId, player.uniqueId, player.name) }
    }
}
