package dev.slne.surf.odyssey.runtime.paper.progression.tool

import dev.slne.surf.odyssey.core.common.progression.ToolSightingResult
import kotlinx.coroutines.test.runTest
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.io.IOException
import java.util.*
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for reporting tool sightings and reacting to suspected duplicates. */
class SightingReporterTest {
    /** The reported tool. */
    private val tool = UUID.randomUUID()

    /** The player holding the tool. */
    private val holder = UUID.randomUUID()

    /** The log records written by the reporter. */
    private val records = mutableListOf<LogRecord>()

    /** A logger that keeps its records in [records]. */
    private val logger = Logger.getAnonymousLogger().apply {
        useParentHandlers = false
        level = Level.ALL
        addHandler(object : Handler() {
            /** Keeps [record]. */
            override fun publish(record: LogRecord) {
                records += record
            }

            /** Does nothing. */
            override fun flush() = Unit

            /** Does nothing. */
            override fun close() = Unit
        })
    }

    /** The messages sent to the online admins. */
    private val adminMessages = mutableListOf<Component>()

    /** The service failures passed on by the reporter. */
    private val failures = mutableListOf<Throwable>()

    /** Creates a reporter whose service answers every sighting with [answer]. */
    private fun reporter(answer: suspend (UUID, UUID, String) -> ToolSightingResult) = SightingReporter(
        report = answer,
        serverName = { "hub-north-01" },
        logger = logger,
        notifyAdmins = { adminMessages += it },
        onFailure = { failures += it },
    )

    /** Returns the warnings among [records]. */
    private fun warnings() = records.filter { it.level == Level.WARNING }

    @Test
    fun `an unremarkable sighting logs nothing and tells nobody`() = runTest {
        var sent: Triple<UUID, UUID, String>? = null
        reporter { t, h, s -> sent = Triple(t, h, s); ToolSightingResult.OK }.report(tool, "pick", holder, "Alice")

        assertEquals(Triple(tool, holder, "hub-north-01"), sent)
        assertTrue(warnings().isEmpty())
        assertTrue(adminMessages.isEmpty())
    }

    @Test
    fun `a suspected duplicate is logged and told to the admins`() = runTest {
        reporter { _, _, _ -> ToolSightingResult.DUPLICATE_SUSPECTED }.report(tool, "pick", holder, "Alice")

        assertEquals(1, warnings().size)
        assertTrue(warnings().single().message.contains(tool.toString()))
        val text = PlainTextComponentSerializer.plainText().serialize(adminMessages.single())
        assertTrue(text.contains("Alice") && text.contains(tool.toString()), text)
    }

    @Test
    fun `an unknown tool is logged once`() = runTest {
        val reporter = reporter { _, _, _ -> ToolSightingResult.UNKNOWN_TOOL }
        reporter.report(tool, "pick", holder, "Alice")
        reporter.report(tool, "pick", holder, "Alice")

        assertEquals(1, warnings().size)
        assertTrue(adminMessages.isEmpty())
    }

    @Test
    fun `a failed report is passed on`() = runTest {
        reporter { _, _, _ -> throw IOException("down") }.report(tool, "pick", holder, "Alice")

        assertEquals("down", failures.single().message)
        assertTrue(adminMessages.isEmpty())
    }
}
