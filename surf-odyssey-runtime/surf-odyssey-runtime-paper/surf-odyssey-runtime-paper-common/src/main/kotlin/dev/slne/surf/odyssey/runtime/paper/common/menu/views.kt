package dev.slne.surf.odyssey.runtime.paper.common.menu

import dev.slne.surf.api.paper.inventory.framework.register
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeDetailView
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeListView
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeView
import dev.slne.surf.odyssey.runtime.paper.common.menu.skill.view.skillDetailView
import dev.slne.surf.odyssey.runtime.paper.common.menu.skill.view.skillView

/** Registers every inventory view of the common module; must run during plugin load. */
internal fun registerViews() {
    homeView.register()
    homeListView.register()
    homeDetailView.register()
    skillView.register()
    skillDetailView.register()
}
