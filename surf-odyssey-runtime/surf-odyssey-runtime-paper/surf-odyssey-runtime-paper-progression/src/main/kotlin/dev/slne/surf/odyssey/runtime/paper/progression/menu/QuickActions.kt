@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.menu

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.event.RegistryEvents
import io.papermc.paper.registry.keys.tags.DialogTagKeys
import io.papermc.paper.tag.TagEntry

/** Puts the [OdysseyMenuDialog] behind the client's quick actions key. */
object QuickActions {
    /**
     * Registers the [OdysseyMenuDialog] in the dialog registry and adds it to the tag `#minecraft:quick_actions`.
     * Must be called from the bootstrap of a plugin, before the registries are frozen.
     *
     * @param context the bootstrap context of the plugin
     */
    fun bootstrap(context: BootstrapContext) {
        val lifecycle = context.lifecycleManager
        lifecycle.registerEventHandler(RegistryEvents.DIALOG.compose().newHandler { event ->
            event.registry().register(OdysseyMenuDialog.TYPED_KEY, OdysseyMenuDialog::configure)
        })
        lifecycle.registerEventHandler(LifecycleEvents.TAGS.preFlatten(RegistryKey.DIALOG)) { event ->
            event.registrar().addToTag(
                DialogTagKeys.QUICK_ACTIONS,
                listOf(TagEntry.valueEntry(OdysseyMenuDialog.TYPED_KEY))
            )
        }
    }
}

/** Plugin bootstrap of the hub and home plugins: puts the Odyssey menu behind the quick actions key. */
class OdysseyBootstrap : PluginBootstrap {
    /** Registers the quick action through [QuickActions.bootstrap]. */
    override fun bootstrap(context: BootstrapContext) {
        QuickActions.bootstrap(context)
    }
}
