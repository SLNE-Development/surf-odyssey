pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.slne.surf.api.gradle.settings") version "+"
}

rootProject.name = "surf-odyssey"
include("surf-odyssey-core:surf-odyssey-core-common")
include("surf-odyssey-core:surf-odyssey-core-client")

include("surf-odyssey-runtime:surf-odyssey-runtime-paper:surf-odyssey-runtime-paper-common")
include("surf-odyssey-runtime:surf-odyssey-runtime-paper:surf-odyssey-runtime-paper-home")
include("surf-odyssey-runtime:surf-odyssey-runtime-paper:surf-odyssey-runtime-paper-hub:surf-odyssey-runtime-paper-hub-common")
include("surf-odyssey-runtime:surf-odyssey-runtime-paper:surf-odyssey-runtime-paper-hub:surf-odyssey-runtime-paper-hub-lobby")

include("surf-odyssey-runtime:surf-odyssey-runtime-velocity")
include("surf-odyssey-runtime:surf-odyssey-runtime-microservice")
