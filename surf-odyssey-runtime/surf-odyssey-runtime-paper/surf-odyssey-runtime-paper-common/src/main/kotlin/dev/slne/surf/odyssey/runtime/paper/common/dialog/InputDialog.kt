@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.common.dialog

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.builder.DialogActionButtonBuilder
import dev.slne.surf.api.paper.dialog.builder.DialogBaseBuilder
import dev.slne.surf.api.paper.dialog.builder.DialogBodyBuilder
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import io.papermc.paper.dialog.Dialog
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component

/**
 * Builds a dialog whose inputs come from [schema].
 *
 * Submitting validates the response against the schema. Valid input reaches the submit handler as
 * [SchemaValues]; invalid input reopens the same dialog with the player's input kept and the error
 * messages shown above the inputs.
 *
 * ```
 * inputDialog(HomeSchema) {
 *     title { primary("Home erstellen") }
 *     initial(HomeSchema.name, home?.name)
 *     submit("Erstellen") { player, values -> ... }
 *     cancel("Abbrechen") { player -> ... }
 * }
 * ```
 */
fun <S : DialogSchema> inputDialog(schema: S, block: InputDialogBuilder<S>.() -> Unit): Dialog =
    InputDialogBuilder(schema).apply(block).build()

class InputDialogBuilder<S : DialogSchema> internal constructor(private val schema: S) {
    private class Button<H>(val label: Component, val handler: H)

    private var title: Component? = null
    private var body: (DialogBodyBuilder.() -> Unit)? = null
    private var base: (DialogBaseBuilder.() -> Unit)? = null
    private val initialValues = mutableMapOf<String, Any>()
    private var submit: Button<(Player, SchemaValues) -> Unit>? = null
    private var cancel: Button<(Player) -> Unit>? = null

    fun title(title: Component) {
        this.title = title
    }

    fun title(block: SurfComponentBuilder.() -> Unit) = title(SurfComponentBuilder(block))

    /** Content shown above the inputs. */
    fun body(block: DialogBodyBuilder.() -> Unit) {
        body = block
    }

    /** Applied last to the dialog base, for settings this builder does not cover. */
    fun base(block: DialogBaseBuilder.() -> Unit) {
        base = block
    }

    /** Prefills [field] with [value]; a null [value] leaves the field's own default. */
    fun <T : Any> initial(field: DialogField<*, T>, value: T?) {
        require(field in schema.fields) { "Field '${field.key}' is not part of this schema" }
        if (value == null) initialValues -= field.key else initialValues[field.key] = field.encode(value)
    }

    fun submit(label: Component, onSubmit: (player: Player, values: SchemaValues) -> Unit) {
        submit = Button(label, onSubmit)
    }

    fun submit(label: String, onSubmit: (player: Player, values: SchemaValues) -> Unit) =
        submit(SurfComponentBuilder { success(label) }, onSubmit)

    /** Adds a cancel button; without one the dialog only shows the submit button. */
    fun cancel(label: Component, onCancel: (player: Player) -> Unit) {
        cancel = Button(label, onCancel)
    }

    fun cancel(label: String, onCancel: (player: Player) -> Unit) =
        cancel(SurfComponentBuilder { text(label) }, onCancel)

    internal fun build(
        entered: Map<String, Any?> = emptyMap(),
        errors: Map<String, String> = emptyMap(),
    ): Dialog {
        val title = requireNotNull(title) { "Input dialog title must be set" }
        val submit = requireNotNull(submit) { "Input dialog submit button must be set" }
        val cancel = cancel

        return dialog {
            base {
                title(title)
                this@InputDialogBuilder.body?.let { body(it) }

                if (errors.isNotEmpty()) {
                    body {
                        for (field in schema.fields) {
                            errors[field.key]?.let { message -> plainMessage { error(message) } }
                        }
                    }
                }

                input {
                    for (field in schema.fields) {
                        field.renderAny(this, entered[field.key] ?: initialValues[field.key])
                    }
                }

                this@InputDialogBuilder.base?.invoke(this)
            }

            type {
                if (cancel == null) {
                    notice { submitButton(submit) }
                } else {
                    confirmation {
                        yes { submitButton(submit) }
                        no {
                            label(cancel.label)
                            action { playerCallback { cancel.handler(it) } }
                        }
                    }
                }
            }
        }
    }

    private fun DialogActionButtonBuilder.submitButton(submit: Button<(Player, SchemaValues) -> Unit>) {
        label(submit.label)
        action {
            customPlayerClick { response, player ->
                when (val result = schema.parse(response)) {
                    is SchemaResult.Success -> submit.handler(player, result.values)
                    is SchemaResult.Failure -> player.showDialog(build(result.raw, result.errors))
                }
            }
        }
    }
}
