@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.menu

import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry
import io.papermc.paper.registry.keys.DialogKeys
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player

/**
 * The static "Odyssey" dialog with the buttons "Quests öffnen", "Titel wählen" and "Abholfach".
 *
 * The dialog is registered in the dialog registry under [KEY] and added to the quick actions of the client. Its
 * buttons send the custom click ids [OPEN_QUESTS], [OPEN_TITLES] and [OPEN_PICKUP], which the server answers with
 * the player's own dialog or view.
 */
object OdysseyMenuDialog {
    /** The key of the dialog in the dialog registry. */
    val KEY: Key = Key.key("odyssey", "menu")

    /** The typed registry key of the dialog. */
    val TYPED_KEY: TypedKey<Dialog> = DialogKeys.create(KEY)

    /** Custom click id of the button that opens the quest book. */
    val OPEN_QUESTS: Key = Key.key("odyssey", "open_quests")

    /** Custom click id of the button that opens the title selection. */
    val OPEN_TITLES: Key = Key.key("odyssey", "open_titles")

    /** Custom click id of the button that opens the pickup box. */
    val OPEN_PICKUP: Key = Key.key("odyssey", "open_pickup")

    /**
     * Fills [builder] with the title and the buttons of the dialog. The buttons only send custom click ids, so the
     * dialog stays usable for every player and every show.
     */
    fun configure(builder: DialogRegistryEntry.Builder) {
        builder.base {
            title { primary("Odyssey") }
            externalTitle { primary("Odyssey") }
            afterAction(DialogBase.DialogAfterAction.CLOSE)
        }
        builder.type {
            multiAction {
                columns(1)
                action {
                    label { text("Quests öffnen") }
                    tooltip { spacer("Deine Quests und ihr Fortschritt") }
                    action { customClick(OPEN_QUESTS) }
                }
                action {
                    label { text("Titel wählen") }
                    tooltip { spacer("Wähle den Titel neben deinem Namen") }
                    action { customClick(OPEN_TITLES) }
                }
                action {
                    label { text("Abholfach") }
                    tooltip { spacer("Gegenstände, die auf dich warten") }
                    action { customClick(OPEN_PICKUP) }
                }
                exitAction { label { spacer("Schließen") } }
            }
        }
    }

    /**
     * Returns the registered dialog, or a dialog with the same content built on the spot when the registry does not
     * hold it.
     */
    fun dialog(): Dialog =
        RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG).get(TYPED_KEY) ?: dialog { configure(this) }

    /** Shows the dialog to [player]. */
    fun show(player: Player) {
        player.showDialog(dialog())
    }
}
