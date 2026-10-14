package dev.slne.surf.odyssey.runtime.paper.common.menu

import dev.slne.surf.api.paper.inventory.framework.register
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeListView
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeView

/** Registers every inventory view of the common module; must run during plugin load. */
internal fun registerViews() {
    homeView.register()
    homeListView.register()
}
