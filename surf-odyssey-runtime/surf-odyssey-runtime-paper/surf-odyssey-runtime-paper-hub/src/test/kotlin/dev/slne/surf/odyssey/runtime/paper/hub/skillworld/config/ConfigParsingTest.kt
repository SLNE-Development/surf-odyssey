package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigParsingTest {
    private enum class Fruit { APPLE, PEAR }

    @Test
    fun `parses known names and reports unknown ones`() {
        val invalid = mutableListOf<String>()
        val parsed = parseNames(
            listOf("apple", "banana", "PEAR"),
            { name -> Fruit.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } },
            { invalid += it },
        )
        assertEquals(listOf(Fruit.APPLE, Fruit.PEAR), parsed)
        assertEquals(listOf("banana"), invalid)
    }
}
