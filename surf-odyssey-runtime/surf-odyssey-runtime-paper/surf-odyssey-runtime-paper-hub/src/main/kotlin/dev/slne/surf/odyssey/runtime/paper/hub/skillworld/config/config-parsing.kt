package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config

fun <T : Any> parseNames(names: Collection<String>, lookup: (String) -> T?, onInvalid: (String) -> Unit): List<T> =
    names.mapNotNull { name -> lookup(name) ?: run { onInvalid(name); null } }
