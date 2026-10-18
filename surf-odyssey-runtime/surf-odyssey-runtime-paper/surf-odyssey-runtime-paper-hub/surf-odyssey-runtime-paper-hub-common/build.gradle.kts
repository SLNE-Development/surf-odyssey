plugins {
    id("dev.slne.surf.api.gradle.paper-raw")
}

surfRawPaperApi {
    withCorePaper()
    withSurfRedis()

    testing {
        mockBukkit = false
    }
}

repositories {
    maven("https://repo.momirealms.net/releases/")
}

dependencies {
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:3.2.0")
    compileOnly("net.momirealms:craft-engine-bukkit:26.9.1")
    compileOnly("net.momirealms:craft-engine-core:26.9.1")
    api(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperCommon)
    api(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperProgression)
}
