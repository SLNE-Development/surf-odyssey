plugins {
    id("dev.slne.surf.api.gradle.paper-raw")
}

surfRawPaperApi {
    testing {
        mockBukkit = false
    }
}

repositories {
    maven("https://repo.momirealms.net/releases/")
    maven("https://reposilite.slne.dev/releases")
}

dependencies {
    compileOnly("dev.slne.surf.npc:surf-npc-api:1.6.11")
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:3.2.0")
    compileOnly("net.momirealms:craft-engine-bukkit:26.9.1")
    compileOnly("net.momirealms:craft-engine-core:26.9.1")
    api(projects.surfOdysseyRuntime.surfOdysseyRuntimePaper.surfOdysseyRuntimePaperCommon)
}
