@file:Suppress("UnstableApiUsage")

package your.plugin.dialog // adjust to your project's package

import dev.slne.surf.api.paper.dialog.builder.DialogInputBuilder
import io.papermc.paper.dialog.DialogResponseView
import net.kyori.adventure.text.Component

/**
 * The outcome of validating a single [DialogField]: either the parsed value or an error message.
 */
sealed interface FieldResult<out T : Any> {
    data class Valid<T : Any>(val value: T) : FieldResult<T>
    data class Invalid(val message: String) : FieldResult<Nothing>
}

/**
 * A typed input of a [DialogSchema].
 *
 * The field reads a raw value of type [R] from the dialog response, decodes it into [T] and then
 * runs its steps in declaration order. Transforms change the value, checks reject it; the first
 * failing check ends validation with its message.
 */
sealed class DialogField<R : Any, T : Any>(val key: String, val label: Component) {
    private sealed interface Step<T> {
        class Transform<T>(val transform: (T) -> T) : Step<T>
        class Check<T>(val message: String, val predicate: (T) -> Boolean) : Step<T>
    }

    private val steps = mutableListOf<Step<T>>()

    /** Replaces the value with the result of [transform] for all following steps. */
    fun transform(transform: (T) -> T) {
        steps += Step.Transform(transform)
    }

    /** Rejects the value with [message] unless [predicate] holds. */
    fun refine(message: String, predicate: (T) -> Boolean) {
        steps += Step.Check(message, predicate)
    }

    /** Whether checks are skipped for [value], e.g. an empty optional string. */
    protected open fun skipsChecks(value: T): Boolean = false

    internal abstract fun read(response: DialogResponseView): R?
    internal abstract fun decode(raw: R?): FieldResult<T>
    internal abstract fun encode(value: T): R
    internal abstract fun render(inputs: DialogInputBuilder, initialValue: R?)

    internal fun validate(raw: R?): FieldResult<T> {
        var value = when (val decoded = decode(raw)) {
            is FieldResult.Invalid -> return decoded
            is FieldResult.Valid -> decoded.value
        }

        for (step in steps) {
            when (step) {
                is Step.Transform -> value = step.transform(value)
                is Step.Check -> if (!skipsChecks(value) && !step.predicate(value)) {
                    return FieldResult.Invalid(step.message)
                }
            }
        }

        return FieldResult.Valid(value)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun validateAny(raw: Any?): FieldResult<T> = validate(raw as R?)

    @Suppress("UNCHECKED_CAST")
    internal fun renderAny(inputs: DialogInputBuilder, initialValue: Any?) = render(inputs, initialValue as R?)
}

/** A text input. An absent response value is read as an empty string. */
class StringField internal constructor(key: String, label: Component) :
    DialogField<String, String>(key, label) {
    private var optional = false
    private var maxLength: Int? = null
    private var width: Int? = null
    private var labelVisible: Boolean? = null
    private var multilineMaxLines: Int? = null
    private var multilineHeight: Int? = null
    private var multiline = false

    /** Removes leading and trailing whitespace. */
    fun trim() = transform { it.trim() }

    /** Lets an empty value pass all checks. */
    fun optional() {
        optional = true
    }

    fun nonEmpty(message: String) = refine(message) { it.isNotEmpty() }

    fun min(length: Int, message: String) = refine(message) { it.length >= length }

    /** Rejects values longer than [length] and limits the client input to the same length. */
    fun max(length: Int, message: String) {
        maxLength = length
        refine(message) { it.length <= length }
    }

    fun regex(regex: Regex, message: String) = refine(message) { regex.matches(it) }

    fun width(width: Int) {
        this.width = width
    }

    fun labelVisible(visible: Boolean) {
        labelVisible = visible
    }

    fun multiline(maxLines: Int? = null, height: Int? = null) {
        multiline = true
        multilineMaxLines = maxLines
        multilineHeight = height
    }

    override fun skipsChecks(value: String) = optional && value.isEmpty()

    override fun read(response: DialogResponseView) = response.getText(key)
    override fun decode(raw: String?) = FieldResult.Valid(raw ?: "")
    override fun encode(value: String) = value

    override fun render(inputs: DialogInputBuilder, initialValue: String?) {
        inputs.text(key) {
            label(this@StringField.label)
            initialValue?.let { initial(it) }
            this@StringField.maxLength?.let { maxLength(it) }
            this@StringField.width?.let { width(it) }
            this@StringField.labelVisible?.let { labelVisible(it) }
            if (this@StringField.multiline) multiline(multilineMaxLines, multilineHeight)
        }
    }
}

/** A checkbox. An absent response value is read as the [default]. */
class BooleanField internal constructor(key: String, label: Component) :
    DialogField<Boolean, Boolean>(key, label) {
    private var default = false

    fun default(value: Boolean) {
        default = value
    }

    override fun read(response: DialogResponseView) = response.getBoolean(key)
    override fun decode(raw: Boolean?) = FieldResult.Valid(raw ?: default)
    override fun encode(value: Boolean) = value

    override fun render(inputs: DialogInputBuilder, initialValue: Boolean?) {
        inputs.boolean(key) {
            label(this@BooleanField.label)
            initial(initialValue ?: default)
        }
    }
}

/** A slider over [range]. An absent response value is read as the [default] or the range start. */
class NumberField internal constructor(
    key: String,
    label: Component,
    private val range: ClosedFloatingPointRange<Float>,
) : DialogField<Float, Float>(key, label) {
    private var default: Float? = null
    private var step: Float? = null
    private var width: Int? = null
    private var labelFormat: String? = null

    fun default(value: Float) {
        default = value
    }

    fun step(step: Float) {
        this.step = step
    }

    fun width(width: Int) {
        this.width = width
    }

    /** A translation key or format string for the slider label, e.g. `"%s: %s"`. */
    fun labelFormat(format: String) {
        labelFormat = format
    }

    override fun read(response: DialogResponseView) = response.getFloat(key)
    override fun decode(raw: Float?) =
        FieldResult.Valid((raw ?: default ?: range.start).coerceIn(range))

    override fun encode(value: Float) = value

    override fun render(inputs: DialogInputBuilder, initialValue: Float?) {
        inputs.numberRange(key, range) {
            label(this@NumberField.label)
            (initialValue ?: default)?.let { initial(it.coerceIn(range)) }
            this@NumberField.step?.let { step(it) }
            this@NumberField.width?.let { width(it) }
            this@NumberField.labelFormat?.let { labelFormat(it) }
        }
    }
}

/** A single-option selector whose options map to values of type [E]. */
class ChoiceField<E : Any> internal constructor(key: String, label: Component) :
    DialogField<String, E>(key, label) {
    private class Option<E>(val key: String, val value: E, val label: Component, val default: Boolean)

    private val options = mutableListOf<Option<E>>()
    private var missingMessage = "Bitte wähle eine Option aus."
    private var width: Int? = null
    private var labelVisible: Boolean? = null

    fun option(key: String, value: E, label: Component, default: Boolean = false) {
        options += Option(key, value, label, default)
    }

    fun option(key: String, value: E, label: String, default: Boolean = false) =
        option(key, value, Component.text(label), default)

    /** The error shown when the response holds no known option key. */
    fun missingMessage(message: String) {
        missingMessage = message
    }

    fun width(width: Int) {
        this.width = width
    }

    fun labelVisible(visible: Boolean) {
        labelVisible = visible
    }

    override fun read(response: DialogResponseView) = response.getText(key)

    override fun decode(raw: String?): FieldResult<E> {
        val option = options.firstOrNull { it.key == raw } ?: return FieldResult.Invalid(missingMessage)
        return FieldResult.Valid(option.value)
    }

    override fun encode(value: E) =
        requireNotNull(options.firstOrNull { it.value == value }?.key) {
            "No option of field '$key' has the value $value"
        }

    override fun render(inputs: DialogInputBuilder, initialValue: String?) {
        require(options.isNotEmpty()) { "Choice field '$key' needs at least one option" }
        inputs.singleOption(key) {
            label(this@ChoiceField.label)
            this@ChoiceField.width?.let { width(it) }
            this@ChoiceField.labelVisible?.let { labelVisible(it) }
            for (entry in options) {
                val selected = if (initialValue != null) entry.key == initialValue else entry.default
                option(entry.key, entry.label, selected)
            }
        }
    }
}
