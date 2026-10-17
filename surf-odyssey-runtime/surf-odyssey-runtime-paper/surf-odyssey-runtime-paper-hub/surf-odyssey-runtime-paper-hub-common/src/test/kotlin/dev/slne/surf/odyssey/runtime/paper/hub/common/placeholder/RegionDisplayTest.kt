package dev.slne.surf.odyssey.runtime.paper.hub.common.placeholder

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

class RegionDisplayTest {
    private fun plain(c: Component) = PlainTextComponentSerializer.plainText().serialize(c)

    @Test
    fun `inside a region world and region are joined`() =
        assertEquals("Norden - Eichenwald", plain(RegionDisplay.combined(Component.text("Norden"), Component.text("Eichenwald"))))

    @Test
    fun `outside a region only the world is shown`() =
        assertEquals("Norden", plain(RegionDisplay.combined(Component.text("Norden"), null)))
}
