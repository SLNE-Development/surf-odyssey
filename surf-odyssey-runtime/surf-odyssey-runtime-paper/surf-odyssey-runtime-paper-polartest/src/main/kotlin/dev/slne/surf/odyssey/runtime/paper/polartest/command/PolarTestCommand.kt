package dev.slne.surf.odyssey.runtime.paper.polartest.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.slne.surf.odyssey.runtime.paper.polartest.PolarTestPermissions
import dev.slne.surf.odyssey.runtime.paper.polartest.steps.PolarTestStep

/**
 * Registers `/polartest <step>` for every [PolarTestStep] and `/polartest run` for [PolarTestStep.runOrder]. Players
 * and the console can run them; players are put into the test world afterwards.
 */
fun polarTestCommand(runner: StepRunner) = commandTree("polartest") {
    withPermission(PolarTestPermissions.COMMAND)

    for (step in PolarTestStep.entries) {
        literalArgument(step.id) {
            anyExecutor { sender, _ -> runner.launch(sender, listOf(step)) }
        }
    }
    literalArgument("run") {
        anyExecutor { sender, _ -> runner.launch(sender, PolarTestStep.runOrder) }
    }
}
