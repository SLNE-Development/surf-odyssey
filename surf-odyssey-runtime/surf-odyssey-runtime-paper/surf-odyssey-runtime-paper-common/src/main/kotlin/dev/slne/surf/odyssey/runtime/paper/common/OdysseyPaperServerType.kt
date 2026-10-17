package dev.slne.surf.odyssey.runtime.paper.common

/** Kind of Paper server an odyssey runtime plugin runs on; each runtime plugin declares exactly one. */
enum class OdysseyPaperServerType {
    /** A hub server, such as the lobby. */
    HUB,

    /** The home server, which hosts the players' own worlds. */
    HOME,

    /** The server that builds the resource pack and uploads it; players never join it. */
    PACK
}