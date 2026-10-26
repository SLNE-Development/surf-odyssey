import dev.slne.surf.api.gradle.util.registerRequired

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

repositories {
    maven("https://reposilite.slne.dev/releases")
    maven("https://repo.momirealms.net/releases/")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.odyssey.runtime.paper.polartest.PolarTestPaperMain")

    serverDependencies {
        // Owns the polartest game modes through the per-plugin API of surf-polar-paper
        registerRequired("surf-polar-paper-runtime-paper")
        // Places and checks the test furniture; loaded first, so the worlds are saved while it still runs
        registerRequired("CraftEngine")
    }

    testing {
        mockBukkit = false
    }
}

dependencies {
    compileOnly("dev.slne.surf.polarpaper:surf-polar-paper-api:1.0.1")
    testImplementation("dev.slne.surf.polarpaper:surf-polar-paper-api:1.0.1")
    compileOnly("net.momirealms:craft-engine-core:26.9.1")
    compileOnly("net.momirealms:craft-engine-bukkit:26.9.1")
}
