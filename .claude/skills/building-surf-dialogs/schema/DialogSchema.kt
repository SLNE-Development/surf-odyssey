@file:Suppress("UnstableApiUsage")

package your.plugin.dialog // adjust to your project's package

import io.papermc.paper.dialog.DialogResponseView
import net.kyori.adventure.text.Component
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

/**
 * Declares the inputs of a dialog and how their values are validated.
 *
 * Fields are declared as delegated properties; the property name becomes the input key and the
 * declaration order becomes the input order:
 *
 * ```
 * object HomeSchema : DialogSchema() {
 *     val name by string("Name") {
 *         trim()
 *         nonEmpty("Der Name darf nicht leer sein.")
 *         max(16, "Der Name darf maximal 16 Zeichen lang sein.")
 *     }
 * }
 * ```
 */
abstract class DialogSchema {
    private val _fields = mutableListOf<DialogField<*, *>>()

    /** All fields in declaration order. */
    val fields: List<DialogField<*, *>> get() = _fields

    protected fun string(label: Component, block: StringField.() -> Unit = {}) =
        field { StringField(it, label).apply(block) }

    protected fun string(label: String, block: StringField.() -> Unit = {}) =
        string(Component.text(label), block)

    protected fun bool(label: Component, block: BooleanField.() -> Unit = {}) =
        field { BooleanField(it, label).apply(block) }

    protected fun bool(label: String, block: BooleanField.() -> Unit = {}) =
        bool(Component.text(label), block)

    protected fun number(
        label: Component,
        range: ClosedFloatingPointRange<Float>,
        block: NumberField.() -> Unit = {},
    ) = field { NumberField(it, label, range).apply(block) }

    protected fun number(
        label: String,
        range: ClosedFloatingPointRange<Float>,
        block: NumberField.() -> Unit = {},
    ) = number(Component.text(label), range, block)

    protected fun <E : Any> choice(label: Component, block: ChoiceField<E>.() -> Unit) =
        field { ChoiceField<E>(it, label).apply(block) }

    protected fun <E : Any> choice(label: String, block: ChoiceField<E>.() -> Unit) =
        choice(Component.text(label), block)

    private fun <F : DialogField<*, *>> field(create: (key: String) -> F) =
        PropertyDelegateProvider<DialogSchema, ReadOnlyProperty<DialogSchema, F>> { _, property ->
            val field = create(property.name)
            require(_fields.none { it.key == field.key }) { "Duplicate dialog field '${field.key}'" }
            _fields += field
            ReadOnlyProperty { _, _ -> field }
        }

    /** Reads every field from [response] and validates it. */
    fun parse(response: DialogResponseView): SchemaResult {
        val raw = fields.associate { it.key to it.read(response) }
        return parse(raw)
    }

    /** Validates already read raw values, keyed by field key. */
    fun parse(raw: Map<String, Any?>): SchemaResult {
        val values = mutableMapOf<String, Any>()
        val errors = linkedMapOf<String, String>()

        for (field in fields) {
            when (val result = field.validateAny(raw[field.key])) {
                is FieldResult.Valid -> values[field.key] = result.value
                is FieldResult.Invalid -> errors[field.key] = result.message
            }
        }

        return if (errors.isEmpty()) {
            SchemaResult.Success(SchemaValues(values), raw)
        } else {
            SchemaResult.Failure(errors, raw)
        }
    }
}

/** The validated values of a [DialogSchema], read by field. */
class SchemaValues internal constructor(private val values: Map<String, Any>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(field: DialogField<*, T>): T =
        requireNotNull(values[field.key]) { "Field '${field.key}' is not part of these values" } as T
}

/** The outcome of [DialogSchema.parse]. [raw] holds the values exactly as the player entered them. */
sealed interface SchemaResult {
    val raw: Map<String, Any?>

    data class Success(val values: SchemaValues, override val raw: Map<String, Any?>) : SchemaResult

    /** [errors] maps each invalid field's key to its first failing message, in field order. */
    data class Failure(val errors: Map<String, String>, override val raw: Map<String, Any?>) :
        SchemaResult {
        fun errorOf(field: DialogField<*, *>): String? = errors[field.key]
    }
}
