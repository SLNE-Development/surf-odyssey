plugins {
    id("dev.slne.surf.api.gradle.velocity")
}

surfVelocityApi {
    withCoreVelocity()
}

velocityPluginFile {
    main.set("dev.slne.surf.odyssey.runtime.velocity.VelocityMain")
}

dependencies {
    api(projects.surfOdysseyCore.surfOdysseyCoreClient)
}
