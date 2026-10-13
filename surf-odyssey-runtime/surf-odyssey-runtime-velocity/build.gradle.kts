plugins {
    id("dev.slne.surf.api.gradle.velocity")
}

velocityPluginFile {
    main.set("dev.slne.surf.odyssey.runtime.velocity.VelocityMain")
}

dependencies {
    api(projects.surfOdysseyCore.surfOdysseyCoreClient)
}