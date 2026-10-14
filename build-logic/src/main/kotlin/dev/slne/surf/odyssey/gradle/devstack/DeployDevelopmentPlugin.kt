package dev.slne.surf.odyssey.gradle.devstack

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import javax.inject.Inject

/**
 * Copies a plugin jar into a dev server's plugins folder. A running server is stopped first, because replacing the jar
 * under a loaded plugin breaks its class loading, and started again afterwards.
 */
abstract class DeployDevelopmentPlugin : DefaultTask() {
    @get:InputFile
    abstract val pluginJar: RegularFileProperty

    @get:OutputFile
    abstract val deployedJar: RegularFileProperty

    /** Compose service of the server. */
    @get:Input
    abstract val service: Property<String>

    /** Folder containing the compose file. */
    @get:Internal
    abstract val composeDirectory: DirectoryProperty

    /** Value of COMPOSE_FILE, so the generated microservice compose file belongs to the same project. */
    @get:Internal
    abstract val composeFiles: Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun deploy() {
        val serverWasRunning = isServerRunning()

        if (serverWasRunning) compose("stop", service.get())
        pluginJar.get().asFile.copyTo(deployedJar.get().asFile, overwrite = true)
        if (serverWasRunning) compose("start", service.get())
    }

    private fun isServerRunning(): Boolean {
        val output = ByteArrayOutputStream()
        val result = execOperations.exec {
            workingDir = composeDirectory.get().asFile
            environment("COMPOSE_FILE", composeFiles.get())
            commandLine("docker", "compose", "ps", "--status", "running", "--services")
            standardOutput = output
            errorOutput = OutputStream.nullOutputStream()
            isIgnoreExitValue = true
        }

        return result.exitValue == 0 && output.toString().lines().any { it.trim() == service.get() }
    }

    private fun compose(vararg arguments: String) {
        execOperations.exec {
            workingDir = composeDirectory.get().asFile
            environment("COMPOSE_FILE", composeFiles.get())
            commandLine("docker", "compose", *arguments)
        }
    }
}
