# Dialog schema layer (zod-style) — reference

A small layer on top of the surf-api dialog DSL. A schema declares the inputs and
their validation rules once; `inputDialog(schema) { … }` renders them, parses the
response into typed values, and on invalid input reopens the same dialog with the
player's input preserved and the error messages shown above the inputs.

## Getting the code

Check the project first: search for `class DialogSchema` / `fun <S : DialogSchema> inputDialog`.
If it exists, import from that package and do not copy anything.

If it is missing, copy the three files from `schema/` in this skill directory into one
package of the target module (any package; fix the `package` line). Dependencies: the
surf-api paper module (`dev.slne.surf.api.paper.dialog.*`) and Paper's dialog API — nothing else.

| File | Contents |
|---|---|
| `DialogField.kt` | `FieldResult`, `DialogField` base, `StringField`, `BooleanField`, `NumberField`, `ChoiceField` |
| `DialogSchema.kt` | `DialogSchema`, `SchemaValues`, `SchemaResult` |
| `InputDialog.kt` | `inputDialog(...)`, `InputDialogBuilder` |

## Declaring a schema

```kotlin
object CreateWarpSchema : DialogSchema() {
    val name by string("Name") {
        trim()
        nonEmpty("Der Name darf nicht leer sein.")
        max(24, "Der Name darf maximal 24 Zeichen lang sein.")
        regex(Regex("[a-zA-Z0-9_-]+"), "Nur Buchstaben, Zahlen, _ und -.")
    }
    val description by string("Beschreibung") {
        multiline(maxLines = 6, height = 120)
        trim()
        optional()
        max(200, "Maximal 200 Zeichen.")
    }
    val public by bool("Öffentlich") { default(true) }
    val radius by number("Radius", 1f..64f) { step(1f); default(8f) }
    val category by choice<WarpCategory>("Kategorie") {
        option("spawn", WarpCategory.SPAWN, "Spawn", default = true)
        option("shop", WarpCategory.SHOP, "Shop")
    }
}
```

- The property name is the input key, and the declaration order is the order on screen. Duplicate keys throw.
- Prefer `object` schemas. A schema instance must be the same one you pass to `inputDialog`, because `initial(...)` checks that the field belongs to it.
- Every factory has a `String` label overload, which becomes `Component.text`, and a `Component` overload.

### Field types

| Factory | Value type | Raw response read | Absent value becomes |
|---|---|---|---|
| `string(label) {}` | `String` | `getText(key)` | `""` |
| `bool(label) {}` | `Boolean` | `getBoolean(key)` | `default` (false) |
| `number(label, a..b) {}` | `Float` | `getFloat(key)` | `default` ?: range start, coerced into range |
| `choice<E>(label) {}` | `E` | `getText(key)` (option key) | **Invalid**: `missingMessage` |

### Rules (run in declaration order; the first failing check wins)

All fields:
- `refine(message) { value -> Boolean }`: a custom check.
- `transform { value -> newValue }`: changes the value for all later steps and for the final result.

`StringField`:
- `trim()`
- `nonEmpty(msg)`, `min(n, msg)`, `max(n, msg)`, `regex(re, msg)`
- `optional()`: an empty value (after earlier transforms) skips every check. The value stays `""`, not null.
- `max(n, msg)` also sets the client-side `maxLength(n)`. The client limit applies to the raw text before `trim`.
- UI: `multiline(maxLines, height)`, `width(1..1024)`, `labelVisible(bool)`

Order matters. Put `trim()` before `nonEmpty`, or input made only of whitespace will pass.

`BooleanField`: `default(bool)`.
`NumberField`: `default(f)`, `step(f)`, `width(n)`, `labelFormat(fmt)`. It renders as a slider, so the range is enforced by the UI.
`ChoiceField<E>`:
- `option(key, value, label: String|Component, default = false)`
- `missingMessage(msg)`, `width(n)`, `labelVisible(bool)`
- At least one option is required; rendering throws without one.

## Building the dialog

```kotlin
fun createWarpDialog(existing: Warp? = null): Dialog = inputDialog(CreateWarpSchema) {
    title { primary(if (existing == null) "Warp erstellen" else "Warp bearbeiten") }
    body { plainMessage { spacer("Lege die Daten deines Warps fest.") } }   // optional, above inputs

    initial(CreateWarpSchema.name, existing?.name)          // null = keep field default
    initial(CreateWarpSchema.category, existing?.category)  // typed: takes WarpCategory

    submit("Erstellen") { player, values ->
        val name: String = values[CreateWarpSchema.name]
        val category: WarpCategory = values[CreateWarpSchema.category]
        plugin.launch { /* suspend work: save, then feedback */ }
    }
    cancel("Abbrechen") { player -> /* reopen previous menu */ }

    base { preventClosingWithEscape() }   // optional: raw DialogBaseBuilder access, applied last
}

player.showDialog(createWarpDialog())
```

| Builder member | Notes |
|---|---|
| `title(Component)` / `title { … }` | Required. |
| `body { … }` | A `DialogBodyBuilder`, shown above the inputs. Calling it again replaces the previous block. |
| `initial(field, value: T?)` | Typed prefill. For a choice field, the value must match an option (otherwise it throws). |
| `submit(label, handler)` | Required. A `String` label is styled `success(...)`. The handler gets `(Player, SchemaValues)`. |
| `cancel(label, handler)` | Optional. With it the dialog is a confirmation dialog (yes/no); without it, a notice dialog with only the submit button. A `String` label is styled `text(...)`. |
| `base { … }` | Escape hatch to `DialogBaseBuilder`: `afterAction`, `externalTitle`, `preventClosingWithEscape`, … |

**Validation flow:** submit → `schema.parse(response)`:
- `Success`: the submit handler runs.
- `Failure`: the dialog is rebuilt with the raw values as initial values and one `error(...)` line per invalid field (in field order), placed between the custom body and the inputs.

You never handle errors yourself.

**Threading:** the submit and cancel handlers run inside the dialog click callback. Put suspend or IO work in a coroutine (`plugin.launch { … }`) like in the example. Don't block inside the handler.

## Parsing without the DSL

```kotlin
when (val result = MySchema.parse(response)) {          // DialogResponseView
    is SchemaResult.Success -> result.values[MySchema.name]
    is SchemaResult.Failure -> result.errorOf(MySchema.name) // or result.errors: Map<key, message>
}
MySchema.parse(mapOf("name" to "  x "))                  // raw map, handy in unit tests
```

`result.raw` always holds what the player typed, keyed by field key.

## Common mistakes

- Caching the result of `inputDialog(...)` in a `val` and showing it again. Click callbacks default to a single use (12h lifetime), so buttons go dead after the first click. Expose a function that builds a fresh dialog per show, as above.
- Using `nonEmpty` without `trim()` first, so whitespace counts as content.
- Expecting `optional()` to produce `null`. It doesn't: the value is `""`.
- Calling `initial()` with a field from another schema. It throws `IllegalArgumentException`.
- Doing database or other suspend work directly in `submit`. Launch a coroutine instead.
- Writing a separate "error dialog" to show validation errors. The layer already reopens the dialog with the errors shown.
- Hand-rolling `response.getText("name")` and a separate validation function. Use a schema instead.
