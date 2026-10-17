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
    server("hub-lobby-01") {
        project = ":surf-odyssey-runtime:surf-odyssey-runtime-paper:surf-odyssey-runtime-paper-hub:surf-odyssey-runtime-paper-hub-lobby"
        directory = layout.projectDirectory.dir("surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-hub/surf-odyssey-runtime-paper-hub-lobby/run/hub-lobby-01")
        requiredPlugins = paperServerPlugins
    }
    server("home") {
        project = ":surf-odyssey-runtime:surf-odyssey-runtime-paper:surf-odyssey-runtime-paper-home"
        directory = layout.projectDirectory.dir("surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-home/run")
        // Player worlds are stored through the polar-paper microservice
        requiredPlugins = paperServerPlugins + listOf("polarpaper", "surf-polar-paper-runtime-paper")
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
