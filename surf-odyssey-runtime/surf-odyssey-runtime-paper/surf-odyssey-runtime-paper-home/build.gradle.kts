import dev.slne.surf.api.gradle.util.registerRequired
import dev.slne.surf.api.gradle.util.registerSoft

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.odyssey.runtime.paper.home.HomePaperMain")
    // Registers the Odyssey menu as quick action
    bootstrapper("dev.slne.surf.odyssey.runtime.paper.progression.menu.OdysseyBootstrap")

    serverDependencies {
        // The odyssey client talks to the microservice through RabbitMQ
        registerRequired("surf-rabbitmq-paper")
        // Player data is synced between the servers by HuskSync
        registerRequired("HuskSync")
        // The Odyssey book is a CraftEngine item
        registerRequired("CraftEngine")
        // The progression tags are registered as MiniPlaceholders expansion when MiniPlaceholders is installed
        registerSoft("MiniPlaceholders")
    }

    testing {
        mockBukkit = false
    }
}

dependencies {
    api(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperCommon)
    implementation(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperContent)
}