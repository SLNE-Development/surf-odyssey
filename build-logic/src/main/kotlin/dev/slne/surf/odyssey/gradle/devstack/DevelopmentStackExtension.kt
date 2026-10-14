package dev.slne.surf.odyssey.gradle.devstack

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Configures the docker compose development stack of the development-stack plugin.
 */
abstract class DevelopmentStackExtension {
    /** Minecraft servers and proxies of compose.yaml that run a plugin of this build. */
    abstract val servers: NamedDomainObjectContainer<DevelopmentServer>

    /** Microservices started next to the infrastructure of compose.yaml. */
    abstract val microservices: NamedDomainObjectContainer<DevelopmentMicroservice>

    /** Schemas of the shared PostgreSQL database created on start besides those of the microservices. */
    abstract val schemas: ListProperty<String>

    fun server(name: String, action: Action<DevelopmentServer>) = servers.register(name, action)

    fun microservice(name: String, action: Action<DevelopmentMicroservice>) = microservices.register(name, action)
}

/**
 * A server of compose.yaml, named like its compose service, that runs the shadow jar of a project of this build.
 */
abstract class DevelopmentServer : Named {
    /** Path of the plugin project whose shadow jar is deployed to the server. */
    abstract val project: Property<String>

    /** Folder the server container runs from. */
    abstract val directory: DirectoryProperty

    /** File name prefixes of plugins the server needs besides this one. They have to be put into plugins/ manually. */
    abstract val requiredPlugins: ListProperty<String>
}

/**
 * A microservice, named like its compose service, that runs either a published [image] or an image built from a
 * [dockerfile] of a local Gradle build.
 */
abstract class DevelopmentMicroservice : Named {
    /** Published image the microservice runs, pulled on every start. Takes precedence over [dockerfile]. */
    abstract val image: Property<String>

    /** Docker build context, the root of the Gradle build the Dockerfile builds. */
    abstract val buildContext: DirectoryProperty

    /** Path of the Dockerfile relative to [buildContext]. */
    abstract val dockerfile: Property<String>

    /** Task of this build run before the image is built, e.g. the one that generates the Dockerfile. */
    abstract val prepareTask: Property<String>

    /**
     * Schema of the shared PostgreSQL database the microservice uses. It is created on start. No database is
     * configured when unset.
     */
    abstract val schema: Property<String>

    /** Whether the microservice gets the SURF_REDIS_* variables of the Valkey service. */
    abstract val redis: Property<Boolean>

    init {
        redis.convention(false)
    }
}
