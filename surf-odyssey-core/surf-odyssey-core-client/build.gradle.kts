import dev.slne.surf.microservice.gradle.plugin.rabbit.RabbitModule

plugins {
    id("dev.slne.surf.api.gradle.core")
    id("dev.slne.surf.microservice")
}

surfMicroservice {
    withClientCommonApi()
    withRabbitModule(RabbitModule.CLIENT_API)
}

dependencies {
    api(projects.surfOdysseyCore.surfOdysseyCoreCommon)
}
