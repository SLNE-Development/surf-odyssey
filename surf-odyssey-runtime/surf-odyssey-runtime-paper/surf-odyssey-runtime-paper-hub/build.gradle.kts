import dev.slne.surf.api.gradle.util.registerRequired

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

repositories {
    maven("https://reposilite.slne.dev/releases")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.odyssey.runtime.paper.hub.HubPaperMain")

    serverDependencies {
        // The odyssey client talks to the microservice through RabbitMQ
        registerRequired("surf-rabbitmq-paper")
        // The region HUD is shown through surf-hud
        registerRequired("surf-hud-paper")
        // Player data is synced between the servers by HuskSync
        registerRequired("HuskSync")
    }

    testing {
        mockBukkit = false
    }
}

dependencies {
    api(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperCommon)
    compileOnly("dev.slne.surf.hud:surf-hud-api:1.0.1")
}

kotlin {
    compilerOptions {
        // Keeps type annotations such as @ThrowExceptions on config properties in the bytecode
        freeCompilerArgs.add("-Xemit-jvm-type-annotations")
    }
}
