package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.momirealms.craftengine.bukkit.api.CraftEngineImages
import net.momirealms.craftengine.core.font.BitmapImage
import net.momirealms.craftengine.core.font.Image
import net.momirealms.craftengine.core.font.ReferenceImage
import net.momirealms.craftengine.core.util.Key as CraftEngineKey

/** Turns an image ID of the resource pack into the font glyph that draws it. */
fun interface GlyphSource {
    /**
     * Returns the glyph of [image].
     *
     * @param image the image ID, such as `odyssey:portrait_brina_neutral`
     * @return the glyph, or `null` if the image is unknown
     */
    fun glyph(image: Key): Component?
}

/** Looks up the portrait glyphs of NPCs. */
object Portraits {
    /** Resolves image IDs to glyphs; reads the images loaded by CraftEngine. */
    @Volatile
    var source: GlyphSource = CraftEngineGlyphs

    /**
     * Returns the portrait of [npc] in [mood], falling back to the neutral portrait when the NPC has none for the mood.
     *
     * @param npc the NPC
     * @param mood the mood name
     * @return the glyph, or an empty component if the NPC has no portrait or the image is unknown
     */
    fun of(npc: NpcDefinition, mood: String): Component {
        val image = npc.portraits[mood] ?: npc.portraits[NpcDefinition.NEUTRAL_MOOD] ?: return Component.empty()
        return source.glyph(image) ?: Component.empty()
    }
}

/** A [GlyphSource] that reads the bitmap images loaded by CraftEngine; reports every image unknown without it. */
private object CraftEngineGlyphs : GlyphSource {
    /**
     * Returns the first glyph of the CraftEngine image with the ID [image], following a reference image to the cell
     * of the bitmap it points to.
     */
    override fun glyph(image: Key): Component? = try {
        var found: Image? = CraftEngineImages.byId(CraftEngineKey.of(image.namespace(), image.value()))
        var row = 0
        var column = 0
        if (found is ReferenceImage) {
            row = found.row()
            column = found.col()
            found = found.image()
        }
        (found as? BitmapImage)?.let { bitmap ->
            Component.text(String(Character.toChars(bitmap.codepointAt(row, column))))
                .font(Key.key(bitmap.font().toString()))
        }
    } catch (_: NoClassDefFoundError) {
        null
    }
}
