package dev.slne.surf.odyssey.runtime.paper.common.menu

import dev.slne.surf.api.paper.inventory.framework.register
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeDetailView
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeListView
import dev.slne.surf.odyssey.runtime.paper.common.menu.home.view.homeView
import dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view.tradeDetailView
import dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view.tradeHistoryView
import dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view.tradeListView
import dev.slne.surf.odyssey.runtime.paper.common.menu.trade.view.tradeView

/** Registers every inventory view of the common module; must run during plugin load. */
internal fun registerViews() {
    homeView.register()
    homeListView.register()
    homeDetailView.register()
    tradeView.register()
    tradeListView.register()
    tradeHistoryView.register()
    tradeDetailView.register()
}
