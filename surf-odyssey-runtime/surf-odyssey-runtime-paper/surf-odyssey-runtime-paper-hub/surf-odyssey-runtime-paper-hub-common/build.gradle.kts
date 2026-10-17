plugins {
    id("dev.slne.surf.api.gradle.paper-raw")
}

surfRawPaperApi {
    withCorePaper()

    testing {
        mockBukkit = false
    }
}

dependencies {
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:3.2.0")
    api(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperCommon)
}
