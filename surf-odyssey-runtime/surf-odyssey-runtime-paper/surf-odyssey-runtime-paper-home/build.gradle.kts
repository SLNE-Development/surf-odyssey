plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.odyssey.runtime.paper.home.HomePaperMain")

    testing {
        mockBukkit = false
    }
}

dependencies {
    api(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperCommon)
}