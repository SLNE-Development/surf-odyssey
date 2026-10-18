package dev.slne.surf.odyssey.runtime.paper.progression.tool

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.odyssey.core.common.progression.ProgressionBridge
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessoryDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextDecoration
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.logging.Level
import net.momirealms.craftengine.core.util.Key as CraftEngineKey

/**
 * Creates tool items and reads and writes the state they carry in their persistent data:
 * [TOOL_ID_KEY], [TOOL_UUID_KEY], [TOOL_USAGE_KEY], [TOOL_CHRONICLE_KEY] (JSON) and [TOOL_ACCESSORIES_KEY] (JSON).
 */
object ToolItems {
    /** Persistent data key `odyssey:tool_id` holding the ID of the tool definition. */
    val TOOL_ID_KEY: NamespacedKey = NamespacedKey("odyssey", "tool_id")

    /** Persistent data key `odyssey:tool_uuid` holding the unique ID of the item. */
    val TOOL_UUID_KEY: NamespacedKey = NamespacedKey("odyssey", "tool_uuid")

    /** Persistent data key `odyssey:tool_usage` holding the total usage as a long. */
    val TOOL_USAGE_KEY: NamespacedKey = NamespacedKey("odyssey", "tool_usage")

    /** Persistent data key `odyssey:tool_chronicle` holding the chronicle as JSON. */
    val TOOL_CHRONICLE_KEY: NamespacedKey = NamespacedKey("odyssey", "tool_chronicle")

    /** Persistent data key `odyssey:tool_accessories` holding the accessory ID per slot ID as a JSON object. */
    val TOOL_ACCESSORIES_KEY: NamespacedKey = NamespacedKey("odyssey", "tool_accessories")

    /** The JSON codec of the accessory map. */
    private val accessoryCodec = MapSerializer(String.serializer(), String.serializer())

    /** The JSON format of the accessory map. */
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Creates a new item of [tool] for [owner]: the CraftEngine item of the tool, or its material when CraftEngine
     * does not know the item, with the tool name, a new unique ID, no usage, [owner] as the first holder, the bonuses
     * of level 1 and the tool lore. The unique ID is registered at the progression service in the background.
     *
     * @param tool the tool definition
     * @param owner the player the tool is created for
     * @return the new tool item
     */
    fun create(tool: ToolDefinition, owner: UUID): ItemStack {
        val item = tool.craftEngineId?.let(::craftEngineStack) ?: ItemStack.of(tool.material)
        item.editMeta { meta -> meta.itemName(tool.name) }
        val ownerName = Bukkit.getOfflinePlayer(owner).name ?: owner.toString()
        val state = ToolState(tool.id, UUID.randomUUID(), 0, Chronicle().withOwner(owner, ownerName), emptyMap())
        write(item, state)
        refresh(item, tool, state)
        register(state, owner)
        return item
    }

    /**
     * Reads the tool state of [item].
     *
     * A chronicle or accessory map that cannot be decoded is read as empty.
     *
     * @param item the item to read, may be `null`
     * @return the state, or `null` if [item] is `null`, empty or not a tool item
     */
    fun read(item: ItemStack?): ToolState? {
        if (item == null || item.isEmpty) return null
        val pdc = item.persistentDataContainer
        val toolId = pdc.get(TOOL_ID_KEY, PersistentDataType.STRING) ?: return null
        val uuid = pdc.get(TOOL_UUID_KEY, PersistentDataType.STRING)?.let(::parseUuid) ?: return null
        val usage = pdc.get(TOOL_USAGE_KEY, PersistentDataType.LONG) ?: 0L
        val chronicle = pdc.get(TOOL_CHRONICLE_KEY, PersistentDataType.STRING)
            ?.let { runCatching { Chronicle.decode(it) }.getOrNull() } ?: Chronicle()
        val accessories = pdc.get(TOOL_ACCESSORIES_KEY, PersistentDataType.STRING)
            ?.let { runCatching { json.decodeFromString(accessoryCodec, it) }.getOrNull() } ?: emptyMap()
        return ToolState(toolId, uuid, usage, chronicle, accessories)
    }

    /**
     * Returns the unique ID of the tool [item], or `null` if it is not a tool item.
     *
     * @param item the item to read, may be `null`
     */
    fun uuidOf(item: ItemStack?): UUID? {
        if (item == null || item.isEmpty) return null
        return item.persistentDataContainer.get(TOOL_UUID_KEY, PersistentDataType.STRING)?.let(::parseUuid)
    }

    /**
     * Writes [state] into the persistent data of [item].
     *
     * @param item the tool item, changed in place
     * @param state the state to store
     */
    fun write(item: ItemStack, state: ToolState) {
        item.editPersistentDataContainer { pdc -> writeTo(pdc, state) }
    }

    /**
     * Applies the bonuses of [state] to [item] and, when [lore] is `true`, renders its lore.
     *
     * @param item the tool item, changed in place
     * @param tool the definition of the tool
     * @param state the state of the item
     * @param lore whether the lore is rendered again
     */
    fun refresh(item: ItemStack, tool: ToolDefinition, state: ToolState, lore: Boolean = true) {
        val level = ToolProgress.levelFor(tool, state.usage)
        ToolBonuses.apply(item, ToolBonuses.resolve(tool, level, accessoriesOf(state)))
        if (!lore) return
        val lines = ToolLore.render(tool, state, ::accessoryDefinition)
            .map { it.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) }
        item.editMeta { meta -> meta.lore(lines) }
    }

    /**
     * Returns the definition of the tool [state] belongs to, or `null` if the engine is not installed or the content
     * has no such tool.
     *
     * @param state the state of a tool item
     */
    fun definitionOf(state: ToolState): ToolDefinition? =
        if (Progression.isInstalled) Progression.runtime.registry.tool(state.toolId) else null

    /**
     * Returns the definitions of the accessories attached according to [state]; unknown accessories are left out.
     *
     * @param state the state of a tool item
     */
    fun accessoriesOf(state: ToolState): List<AccessoryDefinition> =
        state.accessories.values.mapNotNull(::accessoryDefinition)

    /**
     * Returns the slot in the inventory of [player] that holds the tool item with [uuid], or `null` if the player
     * does not hold it. Slots are the indices of the inventory contents, including armour and offhand.
     *
     * @param player the player whose inventory is searched
     * @param uuid the unique ID of the tool item
     */
    fun findSlot(player: Player, uuid: UUID): Int? {
        val contents = player.inventory.contents
        return contents.indices.firstOrNull { uuidOf(contents[it]) == uuid }
    }

    /** Returns the accessory with [id] of the installed content, or `null`. */
    private fun accessoryDefinition(id: String): AccessoryDefinition? =
        if (Progression.isInstalled) Progression.runtime.registry.accessory(id) else null

    /** Writes every key of [state] into [pdc]. */
    private fun writeTo(pdc: PersistentDataContainer, state: ToolState) {
        pdc.set(TOOL_ID_KEY, PersistentDataType.STRING, state.toolId)
        pdc.set(TOOL_UUID_KEY, PersistentDataType.STRING, state.uuid.toString())
        pdc.set(TOOL_USAGE_KEY, PersistentDataType.LONG, state.usage)
        pdc.set(TOOL_CHRONICLE_KEY, PersistentDataType.STRING, state.chronicle.encode())
        pdc.set(TOOL_ACCESSORIES_KEY, PersistentDataType.STRING, json.encodeToString(accessoryCodec, state.accessories))
    }

    /** Registers the new tool item of [state] for [owner] at the progression service in the background. */
    private fun register(state: ToolState, owner: UUID) {
        val plugin = ProgressionModule.plugin
        plugin.launch {
            try {
                ProgressionBridge.registerTool(state.uuid, state.toolId, owner)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Could not register tool ${state.uuid} (${state.toolId}) of $owner", e)
                Progression.reportFailure(plugin.logger, "register tool ${state.uuid}", e)
            }
        }
    }

    /** Parses [value] as a UUID, or returns `null` if it is not one. */
    private fun parseUuid(value: String): UUID? = runCatching { UUID.fromString(value) }.getOrNull()

    /** Builds a single CraftEngine item stack for [key], or returns `null` if CraftEngine does not know the item. */
    private fun craftEngineStack(key: Key): ItemStack? =
        CraftEngineItems.byId(CraftEngineKey.of(key.namespace(), key.value()))?.buildBukkitItem()
}
