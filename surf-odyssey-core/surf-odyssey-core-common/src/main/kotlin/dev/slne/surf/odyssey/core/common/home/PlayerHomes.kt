package dev.slne.surf.odyssey.core.common.home

import it.unimi.dsi.fastutil.objects.ObjectSet
import org.jetbrains.annotations.UnmodifiableView

interface PlayerHomes {
    val homes: @UnmodifiableView ObjectSet<Home>

    suspend fun createHome(home: Home)
    suspend fun saveHome(home: Home)
    suspend fun deleteHome(home: Home)
}