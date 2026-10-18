package dev.slne.surf.odyssey.runtime.paper.pack

import org.bukkit.event.server.ServerLoadEvent
import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks that [PackBuildTrigger] starts the CraftEngine pack workflow once the server has started. */
class PackBuildTriggerTest {
    @Test
    fun `startup dispatches the CraftEngine reload once`() {
        val dispatched = mutableListOf<String>()
        PackBuildTrigger { dispatched += it }.onLoad(ServerLoadEvent.LoadType.STARTUP)
        assertEquals(listOf("ce reload all"), dispatched)
    }

    @Test
    fun `a reload does not dispatch anything`() {
        val dispatched = mutableListOf<String>()
        PackBuildTrigger { dispatched += it }.onLoad(ServerLoadEvent.LoadType.RELOAD)
        assertEquals(emptyList(), dispatched)
    }
}
