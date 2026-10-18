package dev.slne.surf.odyssey.runtime.paper.progression.npc

import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.runtime.paper.progression.definition.FakeContext
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.npc
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.QuestStateView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirements
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component.text
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for which players see an NPC. */
class NpcVisibilityTest {
    private val everyone = npc("olek") {
        name = text("Olek")
        skin("texture", "signature")
        portrait("neutral", Key.key("odyssey", "portrait_olek_neutral"))
        greeting = text("Moin!")
    }

    private val afterIntro = npc("mira") {
        name = text("Mira")
        skin("texture", "signature")
        portrait("neutral", Key.key("odyssey", "portrait_mira_neutral"))
        greeting = text("Hallo.")
        visibleWhen = Requirements.questCompleted("harbor_intro")
    }

    @Test
    fun `an npc without requirement is seen even before the progression is loaded`() {
        assertTrue(NpcVisibility.isVisible(everyone, null, inNpcWorld = true))
        assertTrue(NpcVisibility.isVisible(everyone, FakeContext(), inNpcWorld = true))
    }

    @Test
    fun `an npc with requirement is seen only when it is met`() {
        val done = FakeContext(quests = mapOf("harbor_intro" to QuestStateView(QuestStatus.COMPLETED, 0, 0)))

        assertFalse(NpcVisibility.isVisible(afterIntro, null, inNpcWorld = true))
        assertFalse(NpcVisibility.isVisible(afterIntro, FakeContext(), inNpcWorld = true))
        assertTrue(NpcVisibility.isVisible(afterIntro, done, inNpcWorld = true))
    }

    @Test
    fun `players in another world see no npc`() {
        val done = FakeContext(quests = mapOf("harbor_intro" to QuestStateView(QuestStatus.COMPLETED, 0, 0)))

        assertFalse(NpcVisibility.isVisible(everyone, FakeContext(), inNpcWorld = false))
        assertFalse(NpcVisibility.isVisible(afterIntro, done, inNpcWorld = false))
    }
}
