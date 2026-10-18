package dev.slne.surf.odyssey.runtime.paper.progression.tool

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessoryDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import java.util.*

/** Something a tool took part in that its chronicle records. */
sealed interface ChronicleEvent {
    /**
     * A fish was caught.
     *
     * @property fishName the name of the fish
     * @property sizeCm the size of the fish in centimetres, or `null` if the fish has no size
     */
    data class Catch(val fishName: String, val sizeCm: Int? = null) : ChronicleEvent

    /** A region block was mined or chopped. */
    data object BlockMined : ChronicleEvent

    /** A mob of a mob region was killed. */
    data object MobKilled : ChronicleEvent
}

/**
 * One holder of a tool in its owner chain.
 *
 * @property id the UUID of the player as a string
 * @property name the name of the player when the tool was taken over
 */
@Serializable
data class ChronicleOwner(val id: String, val name: String)

/**
 * A remembered catch.
 *
 * @property fish the name of the fish
 * @property sizeCm the size in centimetres, or `null` if the fish has no size
 */
@Serializable
data class CatchRecord(val fish: String, val sizeCm: Int? = null)

/**
 * The history of one tool item: who held it, what it did and its notable catches.
 *
 * The owner chain keeps the newest [MAX_OWNERS] holders; [hasEarlierOwners] tells whether older holders were dropped.
 *
 * @property owners the newest holders, oldest first
 * @property hasEarlierOwners whether holders before [owners] exist
 * @property catches how many fish were caught
 * @property blocksMined how many region blocks were mined or chopped
 * @property mobsKilled how many region mobs were killed
 * @property firstCatch the first fish caught, or `null`
 * @property biggestCatch the biggest fish with a size caught, or `null`
 */
@Serializable
data class Chronicle(
    val owners: List<ChronicleOwner> = emptyList(),
    val hasEarlierOwners: Boolean = false,
    val catches: Long = 0,
    val blocksMined: Long = 0,
    val mobsKilled: Long = 0,
    val firstCatch: CatchRecord? = null,
    val biggestCatch: CatchRecord? = null,
) {
    /** The UUID of the newest holder, or `null` if the tool never had one. */
    val lastOwner: UUID?
        get() = owners.lastOrNull()?.id?.let(UUID::fromString)

    /**
     * Returns this chronicle with [id] as the newest holder. Nothing changes when [id] already is the newest holder.
     * When the chain grows beyond [MAX_OWNERS], the oldest holder is dropped and [hasEarlierOwners] is set.
     *
     * @param id the UUID of the holder
     * @param name the current name of the holder
     */
    fun withOwner(id: UUID, name: String): Chronicle {
        if (lastOwner == id) return this
        val chain = owners + ChronicleOwner(id.toString(), name)
        val dropped = chain.size > MAX_OWNERS
        return copy(owners = chain.takeLast(MAX_OWNERS), hasEarlierOwners = hasEarlierOwners || dropped)
    }

    /**
     * Returns this chronicle with [event] recorded: catches, mined blocks and killed mobs each count one. A catch
     * becomes the first catch if there is none yet, and the biggest catch if it has a size larger than the current
     * biggest catch.
     *
     * @param event what the tool took part in
     */
    fun record(event: ChronicleEvent): Chronicle = when (event) {
        is ChronicleEvent.Catch -> {
            val caught = CatchRecord(event.fishName, event.sizeCm)
            val size = event.sizeCm
            val bigger = size != null && size > (biggestCatch?.sizeCm ?: Int.MIN_VALUE)
            copy(
                catches = catches + 1,
                firstCatch = firstCatch ?: caught,
                biggestCatch = if (bigger) caught else biggestCatch,
            )
        }
        ChronicleEvent.BlockMined -> copy(blocksMined = blocksMined + 1)
        ChronicleEvent.MobKilled -> copy(mobsKilled = mobsKilled + 1)
    }

    /**
     * Returns the lore line "Geführt von: A → B" with the holders oldest first, led by "…" when earlier holders were
     * dropped, or `null` if the tool never had a holder.
     */
    fun ownerLine(): Component? {
        if (owners.isEmpty()) return null
        val names = (if (hasEarlierOwners) listOf("…") else emptyList()) + owners.map { it.name }
        return buildText {
            variableKey("Geführt von: ")
            variableValue(names.joinToString(" → "))
        }
    }

    /** Encodes this chronicle as JSON. */
    fun encode(): String = json.encodeToString(serializer(), this)

    /** Holds the size of the owner chain and the JSON codec. */
    companion object {
        /** How many holders the owner chain keeps. */
        const val MAX_OWNERS = 5

        /** The JSON codec of chronicles; unknown keys are ignored. */
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Decodes a chronicle from [encoded] JSON.
         *
         * @throws kotlinx.serialization.SerializationException if [encoded] is not a chronicle
         */
        fun decode(encoded: String): Chronicle = json.decodeFromString(serializer(), encoded)
    }
}

/** Renders the lore of tool items. */
object ToolLore {
    /** Returns the German name of [type]. */
    fun typeName(type: ToolType): String = when (type) {
        ToolType.PICKAXE -> "Spitzhacke"
        ToolType.AXE -> "Axt"
        ToolType.ROD -> "Angel"
        ToolType.SWORD -> "Schwert"
        ToolType.BOW -> "Bogen"
    }

    /**
     * Returns the lore of a tool item: the line "<Typ> · Level N", the usage needed for the next level (absent at the
     * maximum level), the chronicle lines, one line per attached accessory and one line per unlocked empty slot.
     *
     * @param tool the definition of the tool
     * @param state the state stored on the item
     * @param accessories looks up an accessory definition by ID; unknown accessories are left out
     */
    fun render(
        tool: ToolDefinition,
        state: ToolState,
        accessories: (String) -> AccessoryDefinition?,
    ): List<Component> {
        val level = ToolProgress.levelFor(tool, state.usage)
        val lines = mutableListOf<Component>()
        lines += buildText {
            variableKey("${typeName(tool.type)} · ")
            variableValue("Level $level")
        }
        ToolProgress.usageForNextLevel(tool, level)?.let { next ->
            lines += buildText {
                variableKey("Nutzung: ")
                variableValue("${state.usage} / $next")
                spacer(" bis Level ${level + 1}")
            }
        }
        lines += chronicleLines(state.chronicle)
        lines += accessoryLines(tool, level, state, accessories)
        return lines
    }

    /** Returns the counter, catch and owner lines of [chronicle], led by an empty line when there are any. */
    private fun chronicleLines(chronicle: Chronicle): List<Component> {
        val lines = mutableListOf<Component>()
        if (chronicle.catches > 0) lines += counterLine("Fänge", chronicle.catches)
        if (chronicle.blocksMined > 0) lines += counterLine("Abgebaut", chronicle.blocksMined)
        if (chronicle.mobsKilled > 0) lines += counterLine("Besiegt", chronicle.mobsKilled)
        val biggest = chronicle.biggestCatch
        val first = chronicle.firstCatch
        when {
            biggest != null -> lines += catchLine("Größter Fang", biggest)
            first != null -> lines += catchLine("Erster Fang", first)
        }
        chronicle.ownerLine()?.let { lines += it }
        return if (lines.isEmpty()) lines else listOf(Component.empty()) + lines
    }

    /** Returns the line "<label>: <count>". */
    private fun counterLine(label: String, count: Long): Component = buildText {
        variableKey("$label: ")
        variableValue(count)
    }

    /** Returns the line "<label>: <fish>" with the size in centimetres when known. */
    private fun catchLine(label: String, record: CatchRecord): Component = buildText {
        variableKey("$label: ")
        variableValue(record.fish)
        record.sizeCm?.let { spacer(" ($it cm)") }
    }

    /**
     * Returns one line per slot unlocked at [level]: "Zubehör: <name>" for a filled slot and "Freier Slot: <slot>" for
     * an empty one, led by an empty line when there are any.
     */
    private fun accessoryLines(
        tool: ToolDefinition,
        level: Int,
        state: ToolState,
        accessories: (String) -> AccessoryDefinition?,
    ): List<Component> {
        val lines = ToolSlots.unlocked(tool, level).map { slot ->
            val accessory = state.accessories[slot.id]?.let(accessories)
            if (accessory != null) {
                buildText {
                    variableKey("Zubehör: ")
                    append(accessory.item.displayName)
                }
            } else {
                buildText {
                    variableKey("Freier Slot: ")
                    append(slot.name)
                }
            }
        }
        return if (lines.isEmpty()) lines else listOf(Component.empty()) + lines
    }
}
