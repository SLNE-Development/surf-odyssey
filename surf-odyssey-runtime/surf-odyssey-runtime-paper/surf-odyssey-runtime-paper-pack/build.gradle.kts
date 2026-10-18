import dev.slne.surf.api.gradle.util.registerRequired

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.odyssey.runtime.paper.pack.PackPaperMain")

    serverDependencies {
        // The odyssey client talks to the microservice through RabbitMQ
        registerRequired("surf-rabbitmq-paper")
        // Builds the resource pack and uploads it to SeaweedFS
        registerRequired("CraftEngine")
    }

    testing {
        mockBukkit = false
    }
}

dependencies {
    api(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperCommon)
}
