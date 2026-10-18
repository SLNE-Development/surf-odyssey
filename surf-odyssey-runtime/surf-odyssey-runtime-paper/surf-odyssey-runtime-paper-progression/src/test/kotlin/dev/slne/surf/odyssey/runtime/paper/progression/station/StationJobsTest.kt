package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.odyssey.core.common.progression.StationJobDto
import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

/** Tests for picking the station job a click refers to. */
class StationJobsTest {
    /** The recipe of every job. */
    private val recipe = TestPacks.sushiRecipe(rice = 1, fish = 1, duration = 30.seconds)

    /** Returns an open job of [recipe] that is ready at [readyAt]. */
    private fun job(readyAt: Long) =
        StationJobs.OpenJob(StationJobDto(UUID.randomUUID(), "sushi_bar", "sushi", 1, 0, readyAt), recipe)

    @Test
    fun `the job shown in the slot is found even when the order changed`() {
        val first = job(10)
        val second = job(20)
        assertEquals(first, StationJobs.find(listOf(second, first), first.job.id))
    }

    @Test
    fun `a job that is gone is not replaced by another one`() {
        val gone = job(10)
        assertNull(StationJobs.find(listOf(job(20)), gone.job.id))
    }

    @Test
    fun `a slot without a shown job finds nothing`() =
        assertNull(StationJobs.find(listOf(job(10)), null))
}
