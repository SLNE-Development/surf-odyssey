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
     * System property that the first plugin calling [bootstrap] claims. It is a system property because every plugin
     * carries its own copy of this class in its own class loader.
     */
    private const val CLAIM_PROPERTY = "dev.slne.surf.odyssey.menu.registered"

    /**
     * Registers the [OdysseyMenuDialog] in the dialog registry and adds it to the tag `#minecraft:quick_actions`.
     * Must be called from the bootstrap of a plugin, before the registries are frozen.
     *
     * When several plugins of one server call this, only the first one registers `odyssey:menu`; the later calls
     * do nothing, so the dialog is neither registered twice nor listed twice in the quick actions.
     *
     * @param context the bootstrap context of the plugin
     * @return `true` if this call registered the dialog, `false` if another plugin already did
     */
    fun bootstrap(context: BootstrapContext): Boolean {
        val owner = context.pluginMeta.name
        if (System.getProperties().putIfAbsent(CLAIM_PROPERTY, owner) != null) {
            context.logger.info("odyssey:menu is already registered by ${System.getProperty(CLAIM_PROPERTY)}, skipping")
            return false
        }
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
        return true
    }
}

/** Plugin bootstrap of the hub and home plugins: puts the Odyssey menu behind the quick actions key. */
class OdysseyBootstrap : PluginBootstrap {
    /** Registers the quick action through [QuickActions.bootstrap]. */
    override fun bootstrap(context: BootstrapContext) {
        QuickActions.bootstrap(context)
    }
}
