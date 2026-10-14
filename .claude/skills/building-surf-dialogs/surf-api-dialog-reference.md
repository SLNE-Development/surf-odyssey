# surf-api Paper dialog DSL: API reference

Kotlin DSL in `surf-api-paper` (verified against **3.46.2**) that wraps Paper's dialog API
(`io.papermc.paper.dialog.*`, `io.papermc.paper.registry.data.dialog.*`, Minecraft 1.21.6+ dialogs).
Everything below was read from the library sources unless marked **(unverified)**.

Paper's dialog API is `@ApiStatus.Experimental`; the library files carry
`@file:Suppress("UnstableApiUsage")`, and you should do the same in files that build dialogs.

---

## 0. Mental model

```
dialog {                       // DialogRegistryEntry.Builder (Paper) -> io.papermc.paper.dialog.Dialog
    base { ... }               // DialogBaseBuilder: title, body, inputs, escape/afterAction
    type { ... }               // DialogTypeBuilder: notice | confirmation | multiAction | dialogList | serverLinks
}
player.showDialog(dialog)      // Adventure Audience.showDialog(DialogLike); player.closeDialog() closes
```

- `base {}` = what is shown (title, body elements, inputs). `type {}` = which buttons are shown at the bottom.
- Buttons are `ActionButton`s (label, tooltip, width, optional action). The action decides what a click does.
- Inputs are read back only by actions that receive a `DialogResponseView` (`customClick`, `customPlayerClick`)
  or by `commandTemplate` (`$(key)` substitution). Static actions (`callback`, `playerCallback`, `openUrl`, ...)
  do **not** see input values.
- Every builder enforces required fields with `require(...)` at build time, so a missing title/label/action
  throws `IllegalArgumentException` when `dialog {}` runs, not when the player clicks.

---

## 1. Imports cheat sheet

| Symbol | Import |
|---|---|
| `dialog {}`, `Dialog {}` | `dev.slne.surf.api.paper.dialog.dialog` / `...dialog.Dialog` |
| `base {}` (extension on `DialogRegistryEntry.Builder`) | `dev.slne.surf.api.paper.dialog.base` |
| `type {}` (extension on `DialogRegistryEntry.Builder`) | `dev.slne.surf.api.paper.dialog.type` |
| `noticeDialog`, `noticeDialogWithBuilder` | `dev.slne.surf.api.paper.dialog.noticeDialog` / `...noticeDialogWithBuilder` |
| `Player.clearDialogs` | `dev.slne.surf.api.paper.dialog.clearDialogs` (+ opt-in, see 9) |
| Builders + factory functions (`actionButton`, `dialogAction`, `dialogBase`, `dialogType`, `dialogBody`, `dialogInput` and their capitalized aliases) | `dev.slne.surf.api.paper.dialog.builder.*` |
| `composableDialog`, `DialogScope`, `DialogStore` | `dev.slne.surf.api.paper.dialog.composition.*` |
| `paginatedDialog` | `dev.slne.surf.api.paper.dialog.pagination.paginatedDialog` |
| `searchDialog`, `SearchDialogAction`, `SearchInput` | `dev.slne.surf.api.paper.dialog.search.*` |
| `DialogQuery`, `CursorDialogQuery`, `PageResult`, `CursorResult`, `PageState`, `CursorState` | `dev.slne.surf.api.paper.dialog.query.*` |
| `DialogState` | `dev.slne.surf.api.paper.dialog.state.DialogState` |
| `SurfComponentBuilder` | `dev.slne.surf.api.core.messages.builder.SurfComponentBuilder` |
| Paper `Dialog` | `io.papermc.paper.dialog.Dialog` |
| `DialogResponseView` | `io.papermc.paper.dialog.DialogResponseView` |
| `DialogBase.DialogAfterAction` | `io.papermc.paper.registry.data.dialog.DialogBase` |
| `ActionButton` (type) | `io.papermc.paper.registry.data.dialog.ActionButton` |
| `DialogAction`, `DialogActionCallback` | `io.papermc.paper.registry.data.dialog.action.*` |
| `ClickCallback`, `ClickEvent` | `net.kyori.adventure.text.event.*` |

`base`/`type` are top-level extension functions: IDE auto-import sometimes misses them; without the import
`base {}` resolves to Paper's `base(DialogBase)` and fails to compile.
Everything inside the lambdas (`title`, `body`, `input`, `plainMessage`, `action`, ...) is a member and needs no import.
Color helpers (`primary`, `error`, ...) are members of `SurfComponentBuilder` and need no import inside its lambdas.

---

## 2. Entry points (`dev.slne.surf.api.paper.dialog`)

```kotlin
fun dialog(builder: DialogRegistryEntry.Builder.() -> Unit): Dialog   // = Dialog.create { it.empty().apply(builder) }
fun Dialog(builder: DialogRegistryEntry.Builder.() -> Unit): Dialog   // identical alias
fun DialogRegistryEntry.Builder.base(block: DialogBaseBuilder.() -> Unit)
fun DialogRegistryEntry.Builder.type(block: DialogTypeBuilder.() -> Unit)
@NmsUseWithCaution fun Player.clearDialogs(showEmptyDialogBefore: Boolean = false)
```

- `dialog {}` creates an inline (unregistered) dialog at runtime. Always call both `base {}` and `type {}`;
  what Paper does when one is missing is **(unverified)**, expect an exception.
- `this` inside `dialog {}` is Paper's `DialogRegistryEntry.Builder`, so `base(DialogBase)`/`type(DialogType)`
  with prebuilt Paper objects also work.
- Showing: `player.showDialog(dialog)` (Adventure `Audience.showDialog(DialogLike)`), closing: `player.closeDialog()`.
- Vanilla dialogs: `Dialog.SERVER_LINKS`, `Dialog.QUICK_ACTIONS`, `Dialog.CUSTOM_OPTIONS`.

### Common dialogs (`CommonDialogs.kt`)

```kotlin
fun noticeDialog(builder: DialogRegistryEntry.Builder.() -> Unit): Dialog
    // dialog { builder(); type { notice() } }  -> you supply base {}; type is forced to a default notice
fun noticeDialog(title: Component, notice: Component, width: @Range(1, 1024) Int? = null): Dialog
    // title + one plainMessage(notice, width) body, default notice button
fun noticeDialogWithBuilder(title: Component, width: Int? = null, notice: SurfComponentBuilder.() -> Unit): Dialog
```

Note: the lambda `noticeDialog {}` calls `type { notice() }` after your builder, so a `type {}` you set inside
is overwritten. Use plain `dialog {}` if you need a custom notice button.

---

## 3. `base {}`: `DialogBaseBuilder` (`dev.slne.surf.api.paper.dialog.builder`)

| Member | Signature | Notes |
|---|---|---|
| `title` | `var title: Component?`; `fun title(Component)`; `fun title(block: SurfComponentBuilder.() -> Unit)` | **Required**: `require(title != null) { "Dialog base title must not be null" }` |
| `externalTitle` | `var externalTitle: Component?`; `fun externalTitle(Component)`; `fun externalTitle(block: SurfComponentBuilder.() -> Unit)` | Name used where the dialog is referenced from outside (e.g. button label in a `dialogList` / pause menu). Optional. |
| `canCloseWithEscape` | `var canCloseWithEscape: Boolean?` | Unset = Paper/vanilla default (vanilla: `true`, **unverified** for Paper builder). |
| `preventClosingWithEscape` | `fun preventClosingWithEscape(prevent: Boolean = true)` | Sets `canCloseWithEscape = !prevent`. |
| `afterAction` | `var afterAction: DialogBase.DialogAfterAction?`; `fun afterAction(DialogAfterAction)` | `CLOSE` (close dialog, return to previous non-dialog screen), `NONE` (keep screen open), `WAIT_FOR_RESPONSE` (replace with "waiting for response" screen). Unset = default (vanilla `CLOSE`, **unverified**). |
| `body` | `fun body(block: DialogBodyBuilder.() -> Unit)` | Appends; may be called multiple times; empty blocks are ignored. |
| `input` | `fun input(block: DialogInputBuilder.() -> Unit)` | Appends; may be called multiple times; empty blocks ignored. |

Build always calls `.pause(false)` (the singleplayer pause flag is never set; not configurable through the DSL).
Order on screen: body elements, then inputs, then the type's buttons.

Factories: `fun dialogBase(block): DialogBase`, `fun DialogBase(block): DialogBase` (Paper `DialogBase`).

**Gotcha: `title(block)` lambda.** `title { ... }` takes a `SurfComponentBuilder` lambda. Writing
`title { primary("X"); body { ... } }` compiles, because `body` resolves to the outer implicit receiver
(`DialogBaseBuilder`), but it reads as if the body were part of the title. Always put `body {}`/`input {}`
as siblings of `title {}`. The same applies to the library's own `searchDialog`, which nests `input {}` inside
`body {}` and `type {}` inside `base {}`: legal Kotlin, misleading structure.

---

## 4. `body {}`: `DialogBodyBuilder`

| Method | Signature | Notes |
|---|---|---|
| `plainMessage` | `(message: Component, width: @Range(1,1024) Int? = null)` | Text block. |
| `plainMessage` | `(plain: PlainMessageDialogBody)` | Prebuilt Paper body. |
| `plainMessage` | `(width: Int? = null, block: SurfComponentBuilder.() -> Unit)` | `plainMessage { info("...") }` or `plainMessage(300) { ... }`. |
| `plain` | `(block: PlainDialogMessageBuilder.() -> Unit)` | Builder form: `message(Component)` / `message { }` (**required**: `"Dialog body message must not be null"`), `width(Int)` (1..1024), or set `var message` / `var width`. |
| `item` | `(block: ItemDialogMessageBuilder.() -> Unit)` | Item display, see below. |

`ItemDialogMessageBuilder`:

| Member | Notes |
|---|---|
| `item(ItemStack)` / `item(type: ItemType, amount: Int = 1, block: ItemStack.() -> Unit = {})` / `var item` | **Required**: `"Dialog body item must not be null"`. |
| `description(PlainMessageDialogBody)` / `description { message(...); width(...) }` | Text shown next to the item. |
| `simpleDescription(message: Component, width: Int? = null)` / `simpleDescription(width: Int? = null) { ... }` | Shortcut for the description. |
| `showDecorations(Boolean)` | Count, durability bar, etc. |
| `showTooltip(Boolean)` | Hover tooltip. |
| `width(Int)`, `height(Int)` | Range 1..256. |

All properties are also public `var`s. Factories: `dialogBody {}` / `DialogBody {}` return `ObjectList<DialogBody>`.

---

## 5. `input {}`: `DialogInputBuilder`

The `key` is how you read the value later (`response.getText(key)`, `$(key)` in command templates).
Keys must be unique within a dialog. Every input requires a label: `"Dialog input label must not be null"`.

| Method | Signature | Returns |
|---|---|---|
| `text` | `text(key: String, block: TextDialogInput.() -> Unit)` | `DialogInput` (also added) |
| `simpleText` | `simpleText(key, label: Component)`; `simpleText(key) { label builder }` | `Unit` |
| `boolean` | `boolean(key, block: BooleanDialogInput.() -> Unit)` | `DialogInput` |
| `simpleBoolean` | `simpleBoolean(key, label: Component, defaultValue: Boolean? = null)`; `simpleBoolean(key, defaultValue: Boolean? = null) { label builder }` | `Unit` |
| `numberRange` | `<N> numberRange(key, range: ClosedRange<N>, block: NumberRangeDialogInput.() -> Unit) where N: Number, N: Comparable<N>` | `DialogInput` |
| `simpleNumberRange` | `simpleNumberRange(key, range, label: Component)`; `simpleNumberRange(key, range) { label builder }` | `Unit` |
| `singleOption` | `singleOption(key, block: SingleOptionDialogInput.() -> Unit)` | `DialogInput` |
| `addInput` | `addInput(input: DialogInput)` | `Unit` |

`range` bounds are converted with `toFloat()` (`0..64`, `0.5f..2f` both work). Factories: `dialogInput {}` / `DialogInput {}`.

### `TextDialogInput` (inner class)
| Member | Type / range | Notes |
|---|---|---|
| `label(Component)` / `label { }` / `var label` | required | |
| `width(Int)` | 1..1024 | |
| `labelVisible(Boolean)` | | hide label if false |
| `initial(String)` | | prefilled text |
| `maxLength(Int)` | >= 1 | vanilla default 32 **(unverified)**; set it for longer text |
| `multiline(maxLines: Int? = null, height: Int? = null)` / `multiline(MultilineOptions)` | height 1..512 (Paper) | multi-line text box |

### `BooleanDialogInput`
`label` (required), `initial(Boolean)`, `onTrue(String)`, `onFalse(String)`. `onTrue`/`onFalse` are only the
strings substituted into a **command template**; `getBoolean(key)` is unaffected.

### `NumberRangeDialogInput` (constructed with key, start, end from `range`)
`label` (required), `initial(Float)`, `width(Int)` (1..1024), `labelFormat(String)` (translation key or format
string such as `"%s: %s"`; vanilla default `options.generic_value`), `step(Float)` (positive; omit for continuous).

### `SingleOptionDialogInput`
| Member | Notes |
|---|---|
| `label` | required |
| `option(key: String, label: Component, selected: Boolean = false)` | adds an entry |
| `option(key: String, selected: Boolean = false) { label builder }` | |
| `option(entry: OptionEntry)` / `val entries` | Paper `SingleOptionDialogInput.OptionEntry.create(id, display, initial)` |
| `width(Int)` 1..1024, `labelVisible(Boolean)` | |

Only one option may be `selected = true` (Paper doc). Behaviour with zero options **(unverified)**; always add one.
The response value is the **option key** (string), read with `getText(key)`.

---

## 6. `type {}`: `DialogTypeBuilder`

Exactly one type; the last call wins. **Required**: `"Dialog type must be built"`.

| Method | Signature | Meaning |
|---|---|---|
| `notice()` | | One default button (vanilla "OK", closes) |
| `notice(button: ActionButton)` / `notice { button builder }` | | One custom button |
| `confirmation { yes { }; no { } }` | `confirmation(block: DialogConfirmationTypeBuilder.() -> Unit = {})` | Two buttons. **Both required**: `"Yes action must not be null"`, `"No action must not be null"` (the messages say "action" but they check the buttons). |
| `confirmation(yes: ActionButton, no: ActionButton)` | | |
| `multiAction { action { }; exitAction { }; columns(n) }` | `multiAction(block)` | Grid of buttons |
| `multiAction(actions: List<ActionButton>, block = {})`, `multiAction(vararg actions: ActionButton, block = {})` | | |
| `dialogList { ... }` | `dialogList(block)` | Buttons that open other dialogs (label = their `externalTitle`, falling back to title, **unverified**) |
| `dialogList(vararg dialogs: Dialog, block = {})`, `dialogList(vararg keys: TypedKey<Dialog>, block = {})` | | Keys resolved from `RegistryKey.DIALOG` via `getOrThrow` |
| `serverLinks { ... }` | `serverLinks(block)` | Shows the server links (Bukkit `ServerLinks`) |

`DialogConfirmationTypeBuilder`: `yes(ActionButton)`, `yes { }`, `no(ActionButton)`, `no { }`, `var yes`, `var no`.
Vanilla renders yes on the left, no on the right **(unverified)**.

`DialogMultiActionTypeBuilder`: `action(ActionButton)`, `action { }` (appends, order preserved),
`exitAction(ActionButton)` / `exitAction { }` (extra button at the bottom), `columns(Int)` (>= 1; vanilla default 2,
**unverified**). An empty action list is invalid in vanilla **(unverified whether Paper throws)**.

`DialogListTypeBuilder`: `dialog(Dialog)`, `dialog(TypedKey<Dialog>)`, `dialog(vararg TypedKey<Dialog>)`,
`addAll(vararg Dialog)`, `addAll(Iterable<Dialog>)` (stored in a set, duplicates dropped), `exitAction(...)`,
`columns(Int)`, `buttonWidth(Int)` (1..1024). Unset values use Paper defaults.

`ServerLinksTypeBuilder`: `exitAction(...)`, `columns(Int)` **default 2**, `buttonWidth(Int)` **default 150**
(defaults set by the wrapper).

Factories: `dialogType {}` / `DialogType {}`.

Escape key vs exit action: vanilla runs the type's exit action (or the notice button / confirmation "no")
when the dialog is closed with Escape **(unverified; test it before relying on it)**.

---

## 7. Buttons: `DialogActionButtonBuilder`

Used by `notice { }`, `yes { }`, `no { }`, `action { }`, `exitAction { }`, and `actionButton { }`.

| Member | Notes |
|---|---|
| `label(Component)` / `label { }` / `var label` | **Required**: `"ActionButton label must not be null"` |
| `tooltip(Component)` / `tooltip { }` / `var tooltip` | hover text |
| `width(Int)` / `var width` | 1..1024 (vanilla default 150, **unverified**) |
| `action(DialogAction)` / `action { DialogActionBuilder }` / `var action` | Optional: a button without an action just triggers `afterAction` (closes by default). |

Factories: `fun actionButton(block): ActionButton`, `fun ActionButton(block): ActionButton`
(both in `dev.slne.surf.api.paper.dialog.builder`). Use them to build button lists outside the DSL, e.g.
`multiAction(items.map { actionButton { ... } })`.

---

## 8. Actions: `DialogActionBuilder`

Exactly one action per button; the last call wins. **Required**: `"DialogAction must be built"`.

| Method | Signature | Kind | Sees inputs? |
|---|---|---|---|
| `staticAction` | `(clickEvent: ClickEvent<*>)` | static click event | no |
| `showDialog` | `(dialog: DialogLike)` / `(builder: DialogRegistryEntry.Builder.() -> Unit)` | opens another dialog (the lambda builds it eagerly via `dialog {}`) | no |
| `copyToClipboard` | `(text: String)` | | no |
| `openUrl` | `(url: String)` / `(url: java.net.URL)` | client asks for confirmation | no |
| `suggestCommand` | `(command: String)` | puts text in chat input | no |
| `callback` | `(callback: ClickCallback<Audience>)` | Adventure click callback (`ClickEvent.callback`, default options) | no |
| `playerCallback` | `(callback: ClickCallback<Player>)` | `callback` widened to `Player`; non-players get an error message "Only players can use this action!" | no |
| `commandTemplate` | `(template: String)` | runs a command built from inputs: `"/msg $(target) $(text)"`; `$(key)` = input key, booleans become `onTrue`/`onFalse` | via template |
| `customClick` | `(id: Key, additions: BinaryTagHolder? = null)` | sends custom payload `id`; handle with Paper `PlayerCustomClickEvent` (`getIdentifier()`, `getTag()`, `getDialogResponseView()`) | yes (all input values are added to the payload) |
| `customClick` | `(id: Key, additions: CompoundBinaryTag.Builder.() -> Unit)` | as above, additions built as SNBT | yes |
| `customClick` | `(options: ClickCallback.Options? = null, callback: DialogActionCallback)` | server-side callback `(DialogResponseView, Audience) -> Unit` | **yes** |
| `customPlayerClick` | `(options: ClickCallback.Options? = null, callback: (response: DialogResponseView, player: Player) -> Unit)` | as above, `Player`-typed; non-players get the error message | **yes** |

Factories: `fun dialogAction(block): DialogAction`, `fun DialogAction(block): DialogAction`.

`ClickCallback<T>` is a `fun interface` taking the audience: `playerCallback { player -> ... }` (or `it`).
Adventure's `ClickCallback` also offers `.filter(...)` and `.requiringPermission(perm)` for wrapping.

### Reading the response (`io.papermc.paper.dialog.DialogResponseView`)

```kotlin
response.getText("name")        // String?  text input; single option -> selected option KEY
response.getBoolean("public")   // Boolean? boolean input
response.getFloat("radius")     // Float?   number range (convert: .toInt())
response.payload()              // BinaryTagHolder, raw SNBT of all values
```
All getters return `null` when the key does not exist (typo, or wrong getter type **(unverified)**). Validate on the
server: client input is untrusted (length, range, permissions).

### Callback gotchas

- **Default `ClickCallback.Options`** (Adventure source): `uses = 1`, `lifetime = ClickCallback.DEFAULT_LIFETIME` = 12 hours.
  `customClick`/`customPlayerClick` with `options = null` use `ClickCallback.Options.builder().build()`, i.e. those
  defaults; `callback`/`playerCallback` use `ClickEvent.callback(cb)`, also defaults.
  Consequence: each callback fires **once** and the registration is made when the `Dialog` object is **built**.
  If you cache a `Dialog` in a `val` and show it again, its buttons are dead after the first click. Build dialogs
  in a function per show, or pass options:
  `ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofMinutes(10)).build()`.
  How Paper reacts to an expired callback (silently ignored vs. message) is **(unverified)**.
- **Thread**: not verifiable from API sources. Callbacks are triggered by a client packet and are believed to run
  on the server thread that handles that player (main thread on Paper; region/entity thread on Folia)
  **(unverified)**. Do not block in them; no database or network I/O inline.
- **Suspend work**: callbacks are plain non-suspend lambdas. Launch a coroutine:
  `customPlayerClick { response, player -> plugin.launch { ... } }` (MCCoroutine) or `scope.launch { ... }`.
  Read `response` values **before** launching (copy into locals). If the coroutine touches Bukkit state, make sure
  its dispatcher is the correct server/region thread.
- `afterAction` runs on the client regardless of what the callback does. To show a follow-up dialog, just call
  `player.showDialog(next)` in the callback (it replaces the current one); to keep the screen for an async result,
  use `afterAction(WAIT_FOR_RESPONSE)` and show/close when done.
- Re-showing for validation errors: build a fresh dialog with the entered values as `initial(...)` and the error
  messages as body lines, then `player.showDialog(...)` from the submit callback (pattern used by the example
  project's schema layer).

---

## 9. `Player.clearDialogs`

```kotlin
@NmsUseWithCaution
fun Player.clearDialogs(showEmptyDialogBefore: Boolean = false)
```
Closes/clears the player's dialog screen via an NMS bridge. `@NmsUseWithCaution` is a `@RequiresOptIn` (level WARNING):
annotate the caller with `@OptIn(NmsUseWithCaution::class)` (`dev.slne.surf.api.paper.nms.NmsUseWithCaution`).
What exactly `showEmptyDialogBefore` changes is not visible in the API sources **(unverified)**; for normal closing
prefer Adventure's `player.closeDialog()`.

---

## 10. Composition: stateful re-rendering dialogs (`dialog.composition`, `dialog.state`)

```kotlin
interface DialogState                                   // marker; use an immutable data class

suspend fun <S : DialogState> composableDialog(
    player: Player,
    initialState: S,
    scope: CoroutineScope,
    content: suspend DialogScope<S>.() -> Dialog,       // renderer, re-run on every state change
)

class DialogScope<S : DialogState> {
    fun state(): S
    suspend fun setState(transform: S.() -> S)          // replace state, re-render, re-show
    suspend fun <T> remember(vararg keys: Any?, block: suspend CoroutineScope.() -> T): T
}

class DialogStore<S : DialogState>(initialState, playerUuid, renderer, scope) {
    suspend fun open(); suspend fun update(transform: S.() -> S); fun getState(): S
    fun <T> remember(key: List<Any?>, value: Any?); fun <T> recall(key: List<Any?>): T?
}
```

Behaviour (from source):
- `composableDialog` creates a `DialogStore` and immediately renders + shows (`player.showDialog`) the first frame,
  in the **calling coroutine**.
- `setState` sets the new state and re-runs the renderer; the result is shown to `server.getPlayer(uuid)` (skipped
  if offline). It is `suspend`, so inside button callbacks wrap it: `playerCallback { scope.launch { setState { copy(page = 2) } } }`.
- `remember(keys...)` caches by the key list for the store's whole lifetime (no eviction; a cached `null` is
  recomputed every time). Use it for loading data: `remember(state.page, state.search) { repo.load(...) }`.
- No unmount/dispose API; the store lives as long as callbacks referencing it. State is a plain `var`
  (not synchronized): mutate only through `setState`, from one coroutine at a time.
- Each re-render builds a new `Dialog`, so single-use callbacks are refreshed automatically.

---

## 11. Pagination: `paginatedDialog` (`dialog.pagination`)

```kotlin
suspend fun <T> paginatedDialog(
    player: Player,
    query: DialogQuery<PageState, T>,
    titleBuilder: SurfComponentBuilder.(PageState, PageResult<T>) -> Unit =
        { _, r -> if (r.totalPages == 0) text("Keine Ergebnisse") else text("Seite ${r.page} von ${r.totalPages}") },
    searchable: Boolean = false,
    itemBuilder: (T) -> Pair<Component, Dialog>,   // button label + dialog opened on click
    scope: CoroutineScope,
)

fun interface DialogQuery<S : DialogState, T> { suspend fun execute(state: S): PageResult<T> }
data class PageResult<T>(val items: List<T>, val page: Int, val totalPages: Int, val totalItems: Int? = null)
data class PageState(val page: Int = 1, val limit: Int = 10, val search: String? = null) : DialogState
    // init: require(page >= 1), require(limit > 0)
```

What it renders (built on `composableDialog`): a `multiAction` dialog, `columns(1)`, one button per item
(click: `player.showDialog(itemDialog)`), then "Zurück" if `state.page > 1`, "Weiter" if `state.page < result.totalPages`,
and when `searchable`: a text input with key `"search"` (label "Suche", initial = current search) plus
"Suche zurücksetzen" and "Suchen" buttons (search resets to page 1). Labels are hard-coded German; only the title
is customizable.

Notes:
- Initial state is always `PageState()` (page 1, limit 10); `limit` cannot be changed through this function.
  Use `state.limit` and `state.search` in your query.
- `query.execute` results are `remember`ed per `(page, search)`; going back to a page reuses the cached result.
- `itemBuilder` is called for every item on every render, so the item dialogs are built eagerly. Keep them cheap.
- No back button from an item dialog to the list; add one yourself (e.g. a button whose callback re-invokes
  `paginatedDialog`).
- Must be called from a coroutine (it is `suspend`); `scope` is used for button-triggered state changes.
- `CursorDialogQuery`, `CursorResult(items, nextCursor, hasMore)` and `CursorState(cursor, history, limit, search)`
  exist in `dialog.query`, but no cursor-based dialog uses them in 3.46.2.

---

## 12. Search prompt: `searchDialog` (`dialog.search`)

```kotlin
fun searchDialog(
    title: SurfComponentBuilder.() -> Unit,
    externalTitle: SurfComponentBuilder.() -> Unit = title,
    onSearch: (player: Player, query: String) -> Unit,
    onClose: (player: Player, query: String) -> Unit,
    body: DialogBodyBuilder.() -> Unit = { },
    searchButton: SearchDialogAction.() -> Unit = { label = { text("Suchen") }; tooltip = { text("Klicke hier, um die Suche zu starten.") } },
    cancelButton: SearchDialogAction.() -> Unit = { label = { text("Abbrechen") }; tooltip = { text("Klicke hier, um die Suche abzubrechen.") } },
    searchInput: SearchInput.() -> Unit,                     // no default: pass {} at least
    canCloseWithEscape: Boolean = false,
    afterAction: DialogBase.DialogAfterAction = DialogBase.DialogAfterAction.NONE,
): Dialog

data class SearchDialogAction(var label: SurfComponentBuilder.() -> Unit, var tooltip: SurfComponentBuilder.() -> Unit)
data class SearchInput(
    var key: String = "search",
    var label: SurfComponentBuilder.() -> Unit = { text("Suche") },
    var initialValue: String = "",
    var inputModifier: DialogInputBuilder.TextDialogInput.() -> Unit = {},   // e.g. { maxLength(32) }
)
```

Renders a `confirmation` dialog with one text input. Both buttons use `customPlayerClick`: they call
`player.clearDialogs(false)` and then `onSearch(player, query)` / `onClose(player, query)` with
`query = response.getText(key) ?: ""`. Escape is disabled by default, so the cancel button is the way out.
Quirk: the search button is put in the `no` slot and the cancel button in the `yes` slot (layout order may look
reversed). The returned `Dialog` has single-use callbacks: build it per show. Callers do not need the NMS opt-in.

---

## 13. `SurfComponentBuilder` quick reference (surf-api-core)

`SurfComponentBuilder { ... }` (companion `invoke`) builds a `TextComponent`; every `xxx { }` overload in the DSL
calls it. Inside the lambda:

- Text: `text(String | Boolean | Char | Number, color: TextColor? = null, vararg decoration)`, `appendText(...)`,
  `appendSpace()`, `appendNewline()`, `appendNewline(amount)`, `append(Component)`, `append { nested }`,
  `translatable(...)`, `note(any)`, `ellipsis()`.
- Theme colors (each `(String|Boolean|Char|Number, vararg TextDecoration)`): `primary`, `secondary`, `info`,
  `success`, `warning`, `error`, `spacer`, `darkSpacer`, `variableKey`, `variableValue`.
- Prefixes: `appendInfoPrefix()`, `appendSuccessPrefix()`, `appendWarningPrefix()`, `appendErrorPrefix()`,
  and `appendNew<Info|Success|Warning|Error>PrefixedLine { }`.
- Minecraft colors: `white`, `gray`, `darkGray`, `black`, `red`, `darkRed`, `gold`, `yellow`, `green`, `darkGreen`,
  `aqua`, `darkAqua`, `blue`, `darkBlue`, `lightPurple`, `darkPurple`.
- Style: `color(TextColor?)`, `decorate(TextDecoration...)`, `decoration(d, flag)`, `hoverEvent(...)`, `font(Key?)`.
- Collections: `appendCollection(iterable) { e -> Component }`, `appendCollectionNewLine(...)`, `appendMap(...)`.

(Click events inside dialog text are possible via `clickEvent(...)`, but buttons are the intended interaction.)

---

## 14. Worked examples

All examples assume:
```kotlin
@file:Suppress("UnstableApiUsage")
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.api.core.messages.adventure.sendText      // Audience.sendText { ... }
import io.papermc.paper.dialog.Dialog
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
```
`plugin.launch` stands for your coroutine launcher (e.g. MCCoroutine); `homeService`, `homeRepo` etc. are placeholders.

### 14.1 Notice dialog

```kotlin
fun rulesDialog(): Dialog = dialog {
    base {
        title { primary("Server rules") }
        body {
            plainMessage { info("1. Be nice."); appendNewline(); info("2. No griefing.") }
            plainMessage(width = 300) { spacer("Breaking rules leads to a ban.") }
        }
    }
    type {
        notice {
            label { success("Understood") }
            tooltip { text("Close this window") }
        }   // no action: the button just closes (afterAction default)
    }
}

player.showDialog(rulesDialog())

// or the one-liner (import dev.slne.surf.api.paper.dialog.noticeDialogWithBuilder):
player.showDialog(noticeDialogWithBuilder(Component.text("Info")) { info("Saved.") })
```

### 14.2 Confirmation with yes/no actions and inputs

```kotlin
fun deleteHomeDialog(homeName: String): Dialog = dialog {     // built per show: callbacks are single-use
    base {
        title { error("Delete home") }
        body { plainMessage { text("Really delete "); variableValue(homeName); text("?") } }
        input {
            simpleBoolean("alsoBackup", defaultValue = true) { text("Keep a backup") }
            text("reason") {
                label { text("Reason (optional)") }
                maxLength(64)
            }
        }
        preventClosingWithEscape()
    }
    type {
        confirmation {
            yes {
                label { error("Delete") }
                action {
                    customPlayerClick { response, player ->
                        val backup = response.getBoolean("alsoBackup") ?: false   // read before launching
                        val reason = response.getText("reason").orEmpty()
                        plugin.launch {                                         // suspend work off the callback
                            homeService.delete(player.uniqueId, homeName, backup, reason)
                            player.sendText { appendSuccessPrefix(); success("Deleted.") }
                        }
                    }
                }
            }
            no {
                label { text("Cancel") }
                action { playerCallback { it.closeDialog() } }   // or omit action: button just closes
            }
        }
    }
}
```

### 14.3 multiAction menu

```kotlin
import dev.slne.surf.api.paper.dialog.builder.actionButton
import io.papermc.paper.registry.data.dialog.DialogBase

fun mainMenu(player: Player): Dialog = dialog {
    base {
        title { primary("Menu") }
        externalTitle { text("Menu") }
        afterAction(DialogBase.DialogAfterAction.CLOSE)
    }
    type {
        multiAction {
            columns(2)
            action {
                label { text("Homes") }
                action { playerCallback { p -> p.showDialog(homesDialog(p)) } }
            }
            action {
                label { text("Discord") }
                tooltip { text("Opens the invite link") }
                action { openUrl("https://example.com/discord") }
            }
            action {
                label { text("Settings") }
                width(200)
                action { showDialog(settingsDialog()) }          // static, no callback registration
            }
            exitAction { label { spacer("Close") } }
        }
    }
}

// Buttons from data:
type { multiAction(warps.map { w -> actionButton { label { text(w.name) }; action { playerCallback { it.teleport(w.location) } } } }) { columns(3) } }
```

### 14.4 Paginated list

```kotlin
import dev.slne.surf.api.paper.dialog.pagination.paginatedDialog
import dev.slne.surf.api.paper.dialog.query.DialogQuery
import dev.slne.surf.api.paper.dialog.query.PageResult

suspend fun openHomeList(player: Player, scope: CoroutineScope) {
    paginatedDialog(
        player = player,
        query = DialogQuery { state ->
            val (items, total) = homeRepo.page(player.uniqueId, state.page, state.limit, state.search)
            PageResult(items, page = state.page, totalPages = (total + state.limit - 1) / state.limit, totalItems = total)
        },
        titleBuilder = { _, r -> primary("Homes "); spacer("(${r.page}/${r.totalPages})") },
        searchable = true,
        itemBuilder = { home -> Component.text(home.name) to homeDetailDialog(home) },
        scope = scope,
    )
}

// call site (non-suspend context): plugin.launch { openHomeList(player, this) }
```

### 14.5 Search prompt

```kotlin
import dev.slne.surf.api.paper.dialog.search.searchDialog

player.showDialog(searchDialog(
    title = { primary("Find player") },
    onSearch = { p, query -> plugin.launch { showResults(p, query) } },
    onClose = { _, _ -> },
    searchInput = { label = { text("Name") }; inputModifier = { maxLength(16) } },
))
```

---

## 15. Checklist

1. `@file:Suppress("UnstableApiUsage")`; import `dev.slne.surf.api.paper.dialog.base` and `.type`.
2. `base { title ... }` always; every button needs a label; every input needs a label and a unique key.
3. Pick exactly one `type`. Confirmation needs both `yes` and `no`.
4. Need input values? Use `customPlayerClick`/`customClick` (or `commandTemplate`), not `playerCallback`.
5. Build dialogs per show (or pass `UNLIMITED_USES` options): default callbacks are single-use, 12 h lifetime.
6. In callbacks: read the response first, then `launch` for suspend/IO work; validate input server-side.
7. Keep `body {}`/`input {}` as siblings of `title {}`, not inside its lambda.
8. Show with `player.showDialog(d)`, close with `player.closeDialog()`.
