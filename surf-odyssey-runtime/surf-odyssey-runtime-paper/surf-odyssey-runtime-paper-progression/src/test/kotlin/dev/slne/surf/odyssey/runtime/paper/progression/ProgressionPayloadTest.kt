package dev.slne.surf.odyssey.runtime.paper.progression

import dev.slne.surf.odyssey.core.common.progression.NewDeliveryDto
import dev.slne.surf.odyssey.core.common.progression.OperationDto
import dev.slne.surf.odyssey.core.common.progression.OperationResultDto
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.core.common.progression.StationJobDto
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests that the polymorphic progression payloads survive a JSON round trip with the default sealed handling. */
class ProgressionPayloadTest {
    private val json = Json

    @Test
    fun `every state change survives a round trip inside an operation`() {
        val changes: List<StateChange> = listOf(
            StateChange.QuestAccepted("q", stepVersion = 1, period = 7),
            StateChange.QuestStepAdvanced("q", fromStep = 0, toStep = 1, stepVersion = 2),
            StateChange.QuestCompleted("q", fromStep = 1),
            StateChange.CounterSet("q", step = 1, stepVersion = 2, objective = 0, period = 7, value = 5),
            StateChange.RecipeUnlocked("sushi"),
            StateChange.TitleUnlocked("angler"),
            StateChange.JobStarted(StationJobDto(UUID.randomUUID(), "sushi_bar", "sushi", 2, 10L, 20L)),
            StateChange.JobCollected(UUID.randomUUID()),
        )
        val operation = OperationDto(UUID.randomUUID(), UUID.randomUUID(), changes, listOf(NewDeliveryDto("AAAA", "t")))

        val decoded = json.decodeFromString<OperationDto>(json.encodeToString(operation))

        assertEquals(operation, decoded)
        assertEquals(changes, json.decodeFromString<List<StateChange>>(json.encodeToString(changes)))
    }

    @Test
    fun `every operation result survives a round trip`() {
        val results: List<OperationResultDto> = listOf(
            OperationResultDto.Applied(listOf(UUID.randomUUID(), UUID.randomUUID())),
            OperationResultDto.AlreadyApplied(listOf(UUID.randomUUID())),
            OperationResultDto.Rejected("quest q is COMPLETED"),
        )

        results.forEach { result ->
            assertEquals(result, json.decodeFromString<OperationResultDto>(json.encodeToString(result)))
        }
    }
}
