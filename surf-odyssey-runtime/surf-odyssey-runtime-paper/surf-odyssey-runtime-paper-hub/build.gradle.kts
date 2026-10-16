import dev.slne.surf.api.gradle.util.registerRequired

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

repositories {
    maven("https://reposilite.slne.dev/releases")
    maven("https://repo.nexomc.com/releases")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.odyssey.runtime.paper.hub.HubPaperMain")

    serverDependencies {
        // The odyssey client talks to the microservice through RabbitMQ
        registerRequired("surf-rabbitmq-paper")
        // The skill world is loaded from the polar-paper microservice
        registerRequired("surf-polar-paper-runtime-paper")
        // Portal blocks are placed Nexo blocks
        registerRequired("Nexo")
        // The region HUD is shown through surf-hud
        registerRequired("surf-hud-paper")
    }

    testing {
        mockBukkit = false
    }
}

dependencies {
    api(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperCommon)
    compileOnly("dev.slne.surf.polarpaper:surf-polar-paper-api:1.0.1")
    compileOnly("com.nexomc:nexo:1.29.1") { isTransitive = false }
    compileOnly("dev.slne.surf.hud:surf-hud-api:1.0.1")
}

kotlin {
    compilerOptions {
        // Keeps type annotations such as @ThrowExceptions on config properties in the bytecode
        freeCompilerArgs.add("-Xemit-jvm-type-annotations")
    }
}
