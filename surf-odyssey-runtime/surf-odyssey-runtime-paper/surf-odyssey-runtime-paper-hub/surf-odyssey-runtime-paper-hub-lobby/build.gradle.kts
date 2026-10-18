import dev.slne.surf.api.gradle.util.registerRequired

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.odyssey.runtime.paper.hub.lobby.LobbyHubPlugin")
    // Registers the Odyssey menu as quick action
    bootstrapper("dev.slne.surf.odyssey.runtime.paper.progression.menu.OdysseyBootstrap")
    withCorePaper()
    withSurfRedis()

    serverDependencies {
        // The odyssey client talks to the microservice through RabbitMQ
        registerRequired("surf-rabbitmq-paper")
        // Player data is synced between the servers by HuskSync
        registerRequired("HuskSync")
        // The hub tags are registered as MiniPlaceholders expansion
        registerRequired("MiniPlaceholders")
        // Portal surfaces are CraftEngine blocks
        registerRequired("CraftEngine")
        // The progression NPCs are spawned through surf-npc
        registerRequired("surf-npc-paper")
    }

    testing {
        mockBukkit = false
    }
}

dependencies {
    api(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperHub.surfOdysseyRuntimePaperHubCommon)
    implementation(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperContent)
}
