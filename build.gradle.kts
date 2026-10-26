import dev.slne.surf.odyssey.gradle.devstack.DeployDevelopmentPlugin
import dev.slne.surf.odyssey.gradle.devstack.DevelopmentStackExtension

buildscript {
    repositories {
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/public")
    }
    dependencies {
        classpath("dev.slne.surf.api:surf-api-gradle-plugin:+")
        classpath("dev.slne.surf.microservice:surf-microservice-gradle-plugin:+")
    }
}

plugins {
    id("development-stack")
}

allprojects {
    group = "dev.slne.surf.odyssey"
    version = findProperty("version") as String
}

val paperServerPlugins = listOf(
    "surf-api-paper-server", "surf-rabbitmq-paper", "surf-redis-paper", "surf-core-paper",
    "CommandAPI", "LuckPerms", "packetevents", "HuskSync"
)
// surf-npc is downloaded from https://github.com/SLNE-Development/surf-npc/releases (surf-npc-paper-*-all.jar)
val hubServerPlugins = paperServerPlugins + listOf("craft-engine", "MiniPlaceholders", "surf-npc-paper")

/** Adds the dev server [name] running the hub world module [world]. */
fun DevelopmentStackExtension.hubServer(name: String, world: String) = server(name) {
    project = ":surf-odyssey-runtime:surf-odyssey-runtime-paper:surf-odyssey-runtime-paper-hub:surf-odyssey-runtime-paper-hub-$world"
    directory = layout.projectDirectory.dir("surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-hub/surf-odyssey-runtime-paper-hub-$world/run/$name")
    requiredPlugins = hubServerPlugins
}

developmentStack {
    // LuckPerms of the proxy and the servers and HuskSync of the servers, see compose.yaml
    schemas = listOf("luckperms", "husksync")

    server("proxy") {
        project = ":surf-odyssey-runtime:surf-odyssey-runtime-velocity"
        directory = layout.projectDirectory.dir("surf-odyssey-runtime/surf-odyssey-runtime-velocity/run")
        requiredPlugins = listOf(
            "surf-api-velocity", "surf-rabbitmq-velocity", "surf-redis-velocity", "surf-core-velocity",
            "CommandAPI", "LuckPerms", "packetevents"
        )
    }
    hubServer("hub-lobby-01", "lobby")
    hubServer("hub-north-01", "north")
    hubServer("hub-south-01", "south")
    server("home") {
        project = ":surf-odyssey-runtime:surf-odyssey-runtime-paper:surf-odyssey-runtime-paper-home"
        directory = layout.projectDirectory.dir("surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-home/run")
        // Player worlds are stored through the polar-paper microservice, CraftEngine sends the resource pack,
        // MiniPlaceholders provides the progression tags such as <odyssey_title>
        requiredPlugins = paperServerPlugins + listOf("polarpaper", "surf-polar-paper-runtime-paper", "craft-engine", "MiniPlaceholders")
    }
    server("pack-01") {
        project = ":surf-odyssey-runtime:surf-odyssey-runtime-paper:surf-odyssey-runtime-paper-pack"
        directory = layout.projectDirectory.dir("surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-pack/run/pack-01")
        requiredPlugins = listOf("surf-api-paper-server", "surf-rabbitmq-paper", "surf-redis-paper", "surf-core-paper", "CommandAPI", "LuckPerms", "packetevents", "craft-engine")
    }

    microservice("odyssey") {
        buildContext = layout.projectDirectory
        dockerfile = "surf-odyssey-runtime/surf-odyssey-runtime-microservice/Dockerfile"
        prepareTask = ":surf-odyssey-runtime:surf-odyssey-runtime-microservice:generateMicroserviceDockerfile"
        schema = "odyssey"
    }
    microservice("core") {
        image = "ghcr.io/slne-development/surf-core-microservice:latest"
        schema = "core"
    }
    microservice("polar-paper") {
        image = "ghcr.io/slne-development/surf-polar-paper-microservice:latest"
        schema = "polarpaper"
        redis = true
    }
}

// The polartest dev plugin runs next to the home plugin and is only deployed by hand, never with the stack
tasks.register<DeployDevelopmentPlugin>("deployPolarTestPlugin") {
    group = "developmentStack"
    description = "Copies the polartest dev plugin into the plugins folder of the home dev server and restarts it if it is running."
    dependsOn("generateDevelopmentMicroservices")

    val pluginProject = project(":surf-odyssey-runtime:surf-odyssey-runtime-paper:surf-odyssey-runtime-paper-polartest")
    pluginJar = pluginProject.tasks.named<Jar>("shadowJar").flatMap { it.archiveFile }
    deployedJar = layout.projectDirectory.file(
        "surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-home/run/plugins/${pluginProject.name}.jar"
    )
    service = "home"
    composeDirectory = layout.projectDirectory
    composeFiles = layout.buildDirectory.file("development-stack/microservices.compose.yaml")
        .map { "compose.yaml${File.pathSeparator}${it.asFile.absolutePath}" }
}
