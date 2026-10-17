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
    "CommandAPI", "LuckPerms", "packetevents"
)

developmentStack {
    // LuckPerms of the proxy and the servers, see compose.yaml
    schemas = listOf("luckperms")

    server("proxy") {
        project = ":surf-odyssey-runtime:surf-odyssey-runtime-velocity"
        directory = layout.projectDirectory.dir("surf-odyssey-runtime/surf-odyssey-runtime-velocity/run")
        requiredPlugins = listOf(
            "surf-api-velocity", "surf-rabbitmq-velocity", "surf-redis-velocity", "surf-core-velocity",
            "CommandAPI", "LuckPerms", "packetevents"
        )
    }
    server("hub") {
        project = ":surf-odyssey-runtime:surf-odyssey-runtime-paper:surf-odyssey-runtime-paper-hub"
        directory = layout.projectDirectory.dir("surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-hub/run")
        requiredPlugins = paperServerPlugins + listOf("Nexo")
    }
    server("home") {
        project = ":surf-odyssey-runtime:surf-odyssey-runtime-paper:surf-odyssey-runtime-paper-home"
        directory = layout.projectDirectory.dir("surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-home/run")
        // Player worlds are stored through the polar-paper microservice
        requiredPlugins = paperServerPlugins + listOf("polarpaper", "surf-polar-paper-runtime-paper")
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
