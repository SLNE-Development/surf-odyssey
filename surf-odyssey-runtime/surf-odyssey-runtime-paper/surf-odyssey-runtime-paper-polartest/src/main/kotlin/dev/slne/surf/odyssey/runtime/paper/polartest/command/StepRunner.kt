package dev.slne.surf.odyssey.runtime.paper.polartest.command

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.runtime.paper.polartest.PolarTestPaperMain
import dev.slne.surf.odyssey.runtime.paper.polartest.check.CheckReport
import dev.slne.surf.odyssey.runtime.paper.polartest.steps.PolarTestStep
import dev.slne.surf.odyssey.runtime.paper.polartest.steps.PolarTestSteps
import kotlinx.coroutines.CancellationException
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level

/**
 * Runs test steps one after another for a command sender and reports each one as PASS or FAIL in the chat and in the
 * server log. A step that throws fails with the exception instead of stopping the run.
 */
class StepRunner(private val plugin: PolarTestPaperMain, private val steps: PolarTestSteps) {
    private val gate = StepGate()

    /** Runs [toRun] for [sender], or refuses when another run is still going. */
    fun launch(sender: CommandSender, toRun: List<PolarTestStep>) {
        if (!gate.tryEnter()) {
            sender.sendText {
                appendErrorPrefix()
                error("Es läuft bereits ein Polar-Test, warte bis er fertig ist.")
            }
            return
        }

        plugin.launch {
            try {
                val reports = toRun.map { step -> runStep(step).also { report(sender, it) } }
                if (reports.size > 1) summary(sender, reports)
                if (sender is Player) steps.showWorld(sender)
            } finally {
                gate.leave()
            }
        }
    }

    /** Runs [step] and turns an exception into a failed report. */
    private suspend fun runStep(step: PolarTestStep): CheckReport = try {
        steps.run(step)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        plugin.logger.log(Level.WARNING, "polartest step ${step.id} failed", exception)
        CheckReport(step.id).apply { fail("Abgebrochen: ${exception::class.simpleName}: ${exception.message}") }
    }

    /** Sends and logs the PASS or FAIL lines of [report]. */
    private fun report(sender: CommandSender, report: CheckReport) {
        if (report.passed) {
            plugin.logger.info("polartest PASS ${report.step} (${report.checks} checks)")
            sender.sendText {
                appendSuccessPrefix()
                success("PASS ")
                variableValue(report.step)
                success(" (${report.checks} Prüfungen)")
            }
            return
        }

        plugin.logger.warning("polartest FAIL ${report.step}: ${report.failureMessages.joinToString(" | ")}")
        sender.sendText {
            appendErrorPrefix()
            error("FAIL ")
            variableValue(report.step)
        }
        for (message in report.failureMessages) {
            sender.sendText {
                appendErrorPrefix()
                error(message)
            }
        }
    }

    /** Sends and logs how many of [reports] passed. */
    private fun summary(sender: CommandSender, reports: List<CheckReport>) {
        val passed = reports.count { it.passed }
        plugin.logger.info("polartest run finished: $passed/${reports.size} steps passed")
        sender.sendText {
            if (passed == reports.size) appendSuccessPrefix() else appendErrorPrefix()
            info("Polar-Test fertig: ")
            variableValue("$passed/${reports.size}")
            info(" Schritte bestanden")
        }
    }
}
