package dev.slne.surf.odyssey.runtime.paper.common

enum class OdysseyPaperServerType {
    HUB,
    HOME,

    /** The server that builds the resource pack and uploads it; players never join it. */
    PACK
}