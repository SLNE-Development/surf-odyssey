import dev.slne.surf.api.gradle.util.registerRequired

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.odyssey.runtime.paper.hub.north.NorthHubPlugin")
    withCorePaper()
    withSurfRedis()

    serverDependencies {
        // The odyssey client talks to the microservice through RabbitMQ
        registerRequired("surf-rabbitmq-paper")
        // Player data is synced between the servers by HuskSync
        registerRequired("HuskSync")
        // The hub tags are registered as MiniPlaceholders expansion
        registerRequired("MiniPlaceholders")
        // Custom blocks and items of the hub worlds come from CraftEngine
        registerRequired("CraftEngine")
    }

    testing {
        mockBukkit = false
    }
}

dependencies {
    api(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperHub.surfOdysseyRuntimePaperHubCommon)
}
