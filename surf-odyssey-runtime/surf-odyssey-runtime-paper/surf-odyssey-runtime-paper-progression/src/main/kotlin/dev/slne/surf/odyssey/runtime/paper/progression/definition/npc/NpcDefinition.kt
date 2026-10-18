package dev.slne.surf.odyssey.runtime.paper.progression.definition.npc

import dev.slne.surf.odyssey.runtime.paper.progression.definition.Definition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.ProgressionDsl
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.StationDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.Price
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.TradeOffer
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.TradeResult
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component

/**
 * Skin texture of an NPC.
 *
 * @property value base64 texture value
 * @property signature texture signature
 */
data class NpcSkin(val value: String, val signature: String)

/**
 * An NPC players talk to. Where the NPC stands is defined by the world that places it.
 *
 * @property id stable NPC ID
 * @property name name shown above the NPC and in conversations
 * @property skin skin of the NPC
 * @property portraits CraftEngine image ID of the portrait per mood; contains at least [NEUTRAL_MOOD]
 * @property greeting text of the greeting node that opens every conversation
 * @property visibleWhen requirement a player must meet to see the NPC, or `null` if everyone sees it
 * @property trades offers of the NPC; empty if it is no trader
 * @property station station kind the NPC operates, or `null` if it is no station
 */
class NpcDefinition(
    override val id: String,
    val name: Component,
    val skin: NpcSkin,
    val portraits: Map<String, Key>,
    val greeting: Component,
    val visibleWhen: Requirement?,
    val trades: List<TradeOffer>,
    val station: StationDefinition?,
) : Definition {
    /** Returns [id] for log and validation messages. */
    override fun toString(): String = "NpcDefinition($id)"

    /** Holds the mood names known to every NPC. */
    companion object {
        /** Mood whose portrait every NPC must define and which is used when a node has no portrait for its mood. */
        const val NEUTRAL_MOOD: String = "neutral"
    }
}

/**
 * Builds the values of an [NpcDefinition].
 *
 * @property id the NPC ID
 */
@ProgressionDsl
class NpcBuilder(private val id: String) {
    /** Name of the NPC; required. */
    var name: Component? = null

    /** Skin of the NPC; required. */
    var skin: NpcSkin? = null

    /** Greeting text; required. */
    var greeting: Component? = null

    /** Requirement to see the NPC, or `null` if everyone sees it. */
    var visibleWhen: Requirement? = null

    /** Station kind the NPC operates, or `null`. */
    var station: StationDefinition? = null

    /** Portraits added so far by mood. */
    private val portraits = linkedMapOf<String, Key>()

    /** Trade offers added so far. */
    private val trades = mutableListOf<TradeOffer>()

    /**
     * Sets the skin of the NPC.
     *
     * @param value base64 texture value
     * @param signature texture signature
     */
    fun skin(value: String, signature: String) {
        skin = NpcSkin(value, signature)
    }

    /**
     * Adds the portrait shown for a mood.
     *
     * @param mood the mood name, such as `neutral` or `worried`
     * @param image CraftEngine image ID of the portrait
     */
    fun portrait(mood: String, image: Key) {
        portraits[mood] = image
    }

    /**
     * Adds a trade offer.
     *
     * @param id offer ID, unique among the offers of this NPC
     * @param result what the player receives
     * @param price what the player pays
     */
    fun trade(id: String, result: TradeResult, price: Price) {
        trades += TradeOffer(id, result, price)
    }

    /**
     * Creates the definition.
     *
     * @throws IllegalStateException if [name], [skin] or [greeting] is missing
     */
    fun build(): NpcDefinition = NpcDefinition(
        id = id,
        name = checkNotNull(name) { "npc $id needs a name" },
        skin = checkNotNull(skin) { "npc $id needs a skin" },
        portraits = portraits.toMap(),
        greeting = checkNotNull(greeting) { "npc $id needs a greeting" },
        visibleWhen = visibleWhen,
        trades = trades.toList(),
        station = station,
    )
}

/**
 * Defines an NPC.
 *
 * @param id stable NPC ID
 * @param block sets name, skin, portraits, greeting, trades and station
 * @return the NPC definition
 */
fun npc(id: String, block: NpcBuilder.() -> Unit): NpcDefinition = NpcBuilder(id).apply(block).build()
