// Docker compose development stack: the infrastructure, proxy and servers of compose.yaml next to the applying build,
// plus the microservices of the developmentStack extension, which are added through a generated compose file.

import dev.slne.surf.odyssey.gradle.devstack.DeployDevelopmentPlugin
import dev.slne.surf.odyssey.gradle.devstack.DevelopmentStackExtension
import dev.slne.surf.odyssey.gradle.devstack.GenerateMicroserviceCompose
import dev.slne.surf.odyssey.gradle.devstack.GenerateMicroserviceCompose.MicroserviceSpec

val developmentStack = extensions.create<DevelopmentStackExtension>("developmentStack")

val developmentStackGroup = "developmentStack"
val microserviceComposeFile = layout.buildDirectory.file("development-stack/microservices.compose.yaml")

// Every compose call uses both files, so the generated microservices belong to the same compose project
val composeFileList = microserviceComposeFile.map { "compose.yaml${File.pathSeparator}${it.asFile.absolutePath}" }

val generateDevelopmentMicroservices = tasks.register<GenerateMicroserviceCompose>("generateDevelopmentMicroservices") {
    group = developmentStackGroup
    description = "Writes the compose file that adds the configured microservices to the development stack."
    dependsOn(provider { developmentStack.microservices.mapNotNull { it.prepareTask.orNull } })

    microservices = provider {
        developmentStack.microservices.map { microservice ->
            MicroserviceSpec(
                name = microservice.name,
                image = microservice.image.orNull,
                buildContext = microservice.buildContext.orNull?.asFile?.invariantSeparatorsPath,
                dockerfile = microservice.dockerfile.orNull,
                schema = microservice.schema.orNull,
                redis = microservice.redis.get(),
            )
        }
    }
    schemas = developmentStack.schemas
    servers = provider { developmentStack.servers.names.toList() }
    composeFile = microserviceComposeFile
}

val deployDevelopmentPlugin = tasks.register("deployDevelopmentPlugin") {
    group = developmentStackGroup
    description = "Copies the plugins into the plugins folders of their dev servers and restarts the running ones."
}

val checkDevelopmentPlugins = tasks.register("checkDevelopmentPlugins") {
    group = developmentStackGroup
    description = "Fails when a plugin a dev server needs is missing from its plugins folder."
}

developmentStack.servers.all {
    val server = this
    val pluginsDirectory = server.directory.dir("plugins")
    val taskName = server.name.replaceFirstChar { it.uppercase() }

    val deploy = tasks.register<DeployDevelopmentPlugin>("deploy${taskName}DevelopmentPlugin") {
        group = developmentStackGroup
        description = "Copies the plugin into the plugins folder of the ${server.name} dev server and restarts it if it is running."

        val pluginProject = project(server.project.get())
        val shadowJar = pluginProject.tasks.named<Jar>("shadowJar")

        pluginJar = shadowJar.flatMap { it.archiveFile }
        deployedJar = pluginsDirectory.map { it.file("${pluginProject.name}.jar") }
        service = server.name
        composeDirectory = layout.projectDirectory
        composeFiles = composeFileList
    }
    deployDevelopmentPlugin { dependsOn(deploy) }

    val check = tasks.register("check${taskName}DevelopmentPlugins") {
        group = developmentStackGroup
        description = "Fails when a plugin the ${server.name} dev server needs is missing from its plugins folder."

        val requiredPlugins = server.requiredPlugins
        doLast {
            val directory = pluginsDirectory.get().asFile
            val jars = directory.listFiles { file -> file.extension == "jar" }.orEmpty().map { it.name }
            val missing = requiredPlugins.get().filter { plugin -> jars.none { it.startsWith(plugin) } }

            check(missing.isEmpty()) {
                "Missing plugins of the ${server.name} dev server in $directory: ${missing.joinToString()}"
            }
        }
    }
    checkDevelopmentPlugins { dependsOn(check) }
}

tasks.register<Exec>("startDevelopmentStack") {
    group = developmentStackGroup
    description = "Builds the plugins and local microservice images, pulls the published ones and starts the development stack."
    dependsOn(generateDevelopmentMicroservices, deployDevelopmentPlugin, checkDevelopmentPlugins)

    environment("COMPOSE_FILE", composeFileList.get())
    commandLine("docker", "compose", "up", "--build", "--detach", "--remove-orphans")
    doLast {
        println(
            """
            |Development stack started:
            |  Velocity proxy      localhost:25565 (console: docker compose attach proxy)
            |  Hub and home        reachable through the proxy (console: docker compose attach hub / home)
            |  RabbitMQ management http://localhost:15672
            |  PostgreSQL          localhost:5432
            |  Valkey              localhost:6379
            |Ports and credentials can be changed in .env (see .env.example).
            """.trimMargin()
        )
    }
}

tasks.register<Exec>("stopDevelopmentStack") {
    group = developmentStackGroup
    description = "Stops the development stack and keeps its data."
    dependsOn(generateDevelopmentMicroservices)

    environment("COMPOSE_FILE", composeFileList.get())
    commandLine("docker", "compose", "down", "--remove-orphans")
}

tasks.register<Exec>("cleanDevelopmentStack") {
    group = developmentStackGroup
    description = "Stops the development stack and deletes the database, RabbitMQ and Valkey data. The server folders are kept."
    dependsOn(generateDevelopmentMicroservices)

    environment("COMPOSE_FILE", composeFileList.get())
    commandLine("docker", "compose", "down", "--volumes", "--remove-orphans")
}
