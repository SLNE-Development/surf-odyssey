# surf-api Paper inventory GUI framework: API reference

Verified against `surf-api-paper` **3.46.2** sources and the inventory-framework fork it wraps
(`dev.slne.forks.inventoryframework:*:1.1.1`, Java packages `me.devnatan.inventoryframework.*`, "IF" below).
Anything marked *(unverified)* was not confirmed in source.

Package root used below: `dev.slne.surf.api.paper.inventory.framework` (abbreviated `$IF_SURF`).

---

## 0. Coordinate systems (read this first)

Different APIs use different bases. Getting this wrong is the most common bug.

| API | Row | Column | Source of truth |
|---|---|---|---|
| `RenderContext.slot(row, column) { }` (surf ext) / `slot(row, column)` / `slot(row, column, item)` (IF) | **1-based** | **1-based** | IF `SlotConverter.convertSlot`: `max(row-1,0)*9 + max(column-1,0)`. Surf KDoc wrongly says zero-based. `0` is clamped and behaves like `1`. Values > rows/9 throw `IllegalArgumentException`. |
| `RenderContext.slot(slot: Int)` | linear **0-based** index (`row*9+col`, 0..53) | | IF |
| Container DSL `blockCell(column, row)`, `unblockCell`, `ViewBlockCellComponent` | **1-based** (1..6) | **0-based** (0..8), note argument order is *column first* | `require` checks in `ViewBlockCellComponent` |
| `blockRow(row, exemptColumns)` | 1-based | exempt columns 0-based | |
| `blockColumn(column, exemptRows, rows)` | exempt rows 1-based | 0-based | |
| `blockSlot(slot)`, `blockAllSlots(rows, exemptSlots)`, `unblockSlot` | linear 0-based slot | | |
| `rowText(row, ..., columns: IntRange)`, `rowTextAt(row, column, ...)`, `clearRowText(row)` | 1-based (1..6) | 0-based inclusive range / 0-based start | |
| Pagination nav buttons | button row = last row (BOTTOM) or row 1 (TOP) | fixed at 0-based columns **2** (prev) and **6** (next), i.e. 1-based columns 3 and 7 | `PaginationButton` |
| Paginated content area | `paginationContentRows` | 0-based columns **1..7** (7 items per row; columns 0 and 8 stay empty) | `AbstractPaginatedSurfView.paginationRow` |

---

## 1. Creating views

### 1.1 `surfView`

```kotlin
// import dev.slne.surf.api.paper.inventory.framework.view.surfView
inline fun surfView(header: String, block: context(SurfViewContext) () -> Unit): AbstractSurfView
```
- `header`: plain title text (rendered with `headerTextColor`, default `#404040`, centred by default).
- `block` runs **immediately, once**, while the view object is being built (not per player). Inside it you call
  the DSL functions below (`settings`, `onFirstRender`, `state(...)`, ...). They are top-level functions taking a
  context parameter, so each must be **imported** (see import table, section 12).
- Returns an anonymous subclass of `SurfViewDSLImpl : AbstractSurfView : me.devnatan.inventoryframework.View`.
  `surfView` is `inline`, so each call site yields its own anonymous class; IF looks views up by class.
- Every hook (`onFirstRender`, `onClick`, ...) is a single slot: calling it twice **replaces** the earlier callback.

### 1.2 `paginatedSurfView`

```kotlin
// import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
inline fun paginatedSurfView(header: String, block: context(PaginatedSurfViewContext) () -> Unit): AbstractPaginatedSurfView
```
- **Required** in the block: `layoutTarget('X')` and `pagination<T> { ... }` (with a source *and* an item factory).
  Missing ones throw `IllegalStateException` at construction (`"Missing layout target..."`, `"Missing pagination configuration..."`);
  a missing source throws `IllegalArgumentException` from `requireNotNull` inside `pagination`; a missing item factory
  throws `IllegalStateException` when the pagination state is built.
- The layout is generated for you (see 6.3); any `layout { }` you set in `onInit` is overwritten.

### 1.3 Class-based variants

```kotlin
// import dev.slne.surf.api.paper.inventory.framework.view.AbstractSurfView
abstract class AbstractSurfView(defaultHeader: String) : View() {
    open val settings: SurfViewSettings            // default SimpleViewSettings()
    protected open fun onViewInit(config: ViewConfigBuilder)
    protected open fun onViewOpen(open: OpenContext)
    protected open fun onViewRender(render: RenderContext)     // first render
    protected open fun onViewClick(click: SlotClickContext)    // not called for outside clicks when navigateBackOnOutsideClick
    protected open fun onViewClose(close: CloseContext)
    protected open fun onViewUpdate(update: Context)
    context(_: ViewContainerModificationContext) protected open fun containerDefaults()
    protected fun modifyContainer(context: Context, block: context(ViewContainerModificationContext) () -> Unit)
    protected fun updateHeader(context: Context, header: Component, alignment: TextAlignment? = null)
    protected fun updateHeader(context: Context, header: String, alignment: TextAlignment? = null)
}
```
IF's own `onInit/onOpen/onFirstRender/onClick/onClose/onUpdate` are `final` here; override the `onView*` hooks.
For state in a class-based view use IF's inherited `View` factories (`state(..)`, `mutableState(..)`, `lazyState{}`,
`computedState{}`, `initialState()`) directly as properties.

```kotlin
// import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView
abstract class AbstractPaginatedSurfView(header: String) : AbstractSurfView(header) {
    protected abstract val layoutTarget: Char
    override val settings: PaginatedViewSettings   // default PaginatedViewSettings()
    protected abstract fun createPagination(): PaginationStateBuilder<Context, BukkitItemComponentBuilder, *>
    protected open fun onPaginatedInit/Open/Render/Click/Close/Update(...)
    context(_: ViewContainerModificationContext) protected open fun applyContainerDefaults()
}
```
`createPagination()` is called lazily once; build it with IF's `buildLazyPaginationState { ctx -> list }`,
`buildComputedPaginationState`, `buildPaginationState(list)` etc. and `.itemFactory { builder, value -> }`.
`onView*`/`containerDefaults` are `final` in the paginated base.

### 1.4 `SurfViewRef` / `view`

```kotlin
// import dev.slne.surf.api.paper.inventory.framework.view.view
context(ref: SurfViewRef)          val view: SurfViewDSLImpl
context(ctx: PaginatedSurfViewRef) val view: AbstractPaginatedSurfView
fun AbstractSurfViewRef.getRegisteredView(): AbstractSurfView
```
Every lifecycle callback runs with the view's ref as a context parameter, so `view` gives you the view instance
inside callbacks. Accessing it during the DSL block itself throws `IllegalStateException`
("Cannot access view during DSL configuration"). Constructors of refs/contexts are internal; you never create them.

---

## 2. Registering and opening

```kotlin
// import dev.slne.surf.api.paper.inventory.framework.*   (file extensions.kt, JvmName InventoryFrameworkExtensions)
fun View.register()                               // viewFrame.with(this)
fun View.unregister()                             // viewFrame.remove(this)
fun View.open(player: Player)                     // viewFrame.open(javaClass, player)
fun View.open(player: Player, data: Any)          // initial data, read via initialState()/open.initialData
fun View.open(players: Collection<Player>)
fun View.open(players: Collection<Player>, data: Any)
inline fun ViewConfigBuilder.titleBuilder(title: SurfComponentBuilder.() -> Unit): ViewConfigBuilder
inline fun OpenContext.modifyConfig(modifier: ViewConfigBuilder.() -> Unit)
val View.outlineItem: ItemStack                   // GRAY_STAINED_GLASS_PANE with empty display name
val viewFrame: ViewFrame                          // the shared IF ViewFrame (service-loaded ViewFrameAccessor)
```
- **Register every view in `JavaPlugin#onLoad`** (library KDoc says so). Verified reason: IF only initialises views
  inside `ViewFrame.register()` (`initializeViews()`); `with()` after that merely stores the view, so it is never
  initialised. Registering the same instance twice throws `IllegalStateException("View ... already registered")`.
- Opening an unregistered view class throws `IllegalArgumentException("View not registered: ...")`.
- `open(...)` from outside a view **resets** the player's navigation history (section 9).

From inside a view (any `IFConfinedContext`: `SlotClickContext`, `SlotRenderContext`, `OpenContext`, `RenderContext`, ...):
```kotlin
// IF members
openForPlayer(other: Class<out RootView>) / openForPlayer(other, initialData)
closeForPlayer()
back() / back(initialData) / canBack()          // IF's own "previous context" resume, see section 9
update()                                        // re-render the whole context (from a slot context: the parent)
updateTitleForPlayer(title) / resetTitleForPlayer()
// IFContext members
openForEveryone(Class) / closeForEveryone() / updateTitleForEveryone(title)
// surf extensions: import dev.slne.surf.api.paper.inventory.framework.dsl.openForPlayer / openForEveryone
fun IFConfinedContext.openForPlayer(view: RootView)                  // = openForPlayer(view.javaClass)
fun IFConfinedContext.openForPlayer(view: RootView, initialData: Any)
fun IFContext.openForEveryone(other: RootView) / (other, initialData)
```

---

## 3. Settings

### 3.1 Simple views

```kotlin
// import dev.slne.surf.api.paper.inventory.framework.view.settings      (the DSL function)
context(ctx: SurfViewContext) fun settings(block: SimpleViewSettingsBuilder.() -> Unit)
```
`SimpleViewSettingsBuilder` (`$IF_SURF.view.settings.builder`) adds:
- `rows(rows: ViewRows)` / `rows(count: Int)` (1..6; otherwise `IllegalStateException("Invalid layout value")`). Default **`ViewRows.FIVE`**.

### 3.2 Paginated views

```kotlin
context(ctx: PaginatedSurfViewContext) fun settings(block: PaginatedViewSettingsBuilder.() -> Unit)
```
`PaginatedViewSettingsBuilder` adds:

| Function | Default | Notes |
|---|---|---|
| `paginationViewRows(PaginationViewRows)` | `FIVE` (6 rows total) | total rows = content rows + 1 button row |
| `paginationEmptyRows(PaginationEmptyRows)` / `paginationEmptyRows(Int 0..3)` / `noPaginationEmptyRows()` | `NONE` | empty rows at edge opposite the buttons, taken *out of* content rows |
| `paginationButtonPosition(PaginationButtonPosition)` / `paginationButtonsAtBottom()` / `paginationButtonsAtTop()` | `BOTTOM` | |
| `paginationPageIndicator(PaginationPageIndicator?)` / `noPaginationPageIndicator()` | `PaginationPageIndicator.Default` (`"cur/total"` white) | `fun interface { fun render(currentPage: Int, totalPages: Int): Component? }`, both 1-based, min 1; return `null` to hide |
| `paginationSwitchSound(Sound?)` / `noPaginationSwitchSound()` | `minecraft:item.book.page_turn` | adventure `Sound` |

`PaginatedViewSettings` `init` requires `rows - 1 - emptyRows >= 1` (`IllegalArgumentException` otherwise).
There is **no** `rows(...)` on the paginated builder; size comes from `paginationViewRows`.

Enums (`$IF_SURF.view.settings`):
- `ViewRows { ONE..SIX }`, `.rows: Int` (1..6), `ViewRows.byRows(n)` / `byRowsOrNull(n)`.
- `PaginationViewRows { ONE(ViewRows.TWO), TWO(THREE), THREE(FOUR), FOUR(FIVE), FIVE(SIX) }`, `.actualRows`, `.contentRows`.
- `PaginationEmptyRows { NONE, ONE, TWO, THREE }`, `.rows`, `byRows(n)` (throws `IllegalArgumentException` outside 0..3).
- `PaginationButtonPosition { TOP, BOTTOM }`.

### 3.3 Shared builder flags (`SurfViewSettingsBuilder`, both builders)

| Function | Default | Effect |
|---|---|---|
| `cancelOnClick(cancel = true)` | **true** | IF `config.cancelOnClick()` |
| `cancelOnDrag(cancel = true)` | **true** | |
| `cancelOnDrop(cancel = true)` | **true** | |
| `cancelOnPickup(cancel = true)` | **true** | |
| `cancelAllInteractions()` | | sets all four to true (already the default) |
| `navigateBackOnOutsideClick(navigate = true)` | **true** | clicking outside the inventory goes back in history, or closes if there is none (section 9) |
| `font(Key)` | `minecraft:default` | title font; `SurfViewSettingsDefaults.ASCENDED_HEADER_FONT` = `surf:menu_font_normal_ascended` |
| `headerTextAlignment(TextAlignment)` | `CENTER` | `TextAlignment { LEFT, RIGHT, CENTER }` (`...settings.align`) |
| `headerTextColor(TextColor)` | `#404040` | colour for uncoloured header/row text |
| `headerFontMetrics(ViewFontMetrics)` | `ViewFontMetrics.VANILLA` | must match `font` |
| `rowFontMetrics(ViewFontMetrics)` | `ViewFontMetrics.VANILLA_ROW` | `SURF_MENU` for capital-only menu font |
| `backgroundGlyph(render = true)` | **false** | draws a resource-pack background glyph per `ViewRows` |
| `headerGeometry(ViewHeaderGeometry)` / `headerGeometry { copy(...) }` | `ViewHeaderGeometry.DEFAULT` (vanilla 176px) | only for custom inventory textures |
| `rowFonts(Int2ObjectMap<Key>)` / `rowFont(row, Key)` | `surf:menu_font_row_1..6` | fonts used by `rowText` |

Settings are only applied in IF `onInit` (once, at frame registration) and `onOpen`. `config.size(...)` and
`config.type(CHEST)` are applied **after** your `onInit` hook, so you cannot change size/type there.

---

## 4. Lifecycle hooks (DSL)

All in package `$IF_SURF.view` (import each: `...view.onFirstRender`, etc.). `ViewRef` is `SurfViewRef` or
`PaginatedSurfViewRef` and is available as a context parameter inside the block.

```kotlin
context(ctx: AbstractSurfViewContext<ViewRef>)
fun onInit(block: context(ViewRef) ViewConfigBuilder.() -> Unit)        // once per view (frame registration)
fun onOpen(block: context(ViewRef) OpenContext.() -> Unit)              // per open, before render; cancel() to deny
fun onFirstRender(block: context(ViewRef) RenderContext.() -> Unit)     // per open: place items here
fun onClick(block: context(ViewRef) SlotClickContext.() -> Unit)        // view-level click handler
fun onClose(block: context(ViewRef) CloseContext.() -> Unit)            // cancel() keeps it open
fun onUpdate(block: context(ViewRef) Context.() -> Unit)                // after context.update()
fun containerDefaults(block: context(ViewContainerModificationContext, ViewRef) () -> Unit) // header components, per open
context(ref: AbstractSurfViewRef) fun Context.modifyContainer(block: context(ViewContainerModificationContext) () -> Unit)
context(ctx: PaginatedSurfViewContext) fun layoutTarget(target: Char)
```

Order per open (`AbstractSurfView.onOpen`): record history → glyph/title/`containerDefaults` → your `onOpen` →
adopt any title you set via `modifyConfig { title(...) }` as the container header. Then `onFirstRender`.

`onClick`: if `isOutsideClick && navigateBackOnOutsideClick`, the library handles it and your `onClick` is **not**
called. For paginated DSL views `onFirstRender` runs after the nav buttons are placed.

`onClose`: if your `onClose` cancels, stateful button `onCloseChanged` handlers are skipped.

Cancel helpers (`$IF_SURF.dsl`): `IFSlotClickContext.cancel()`, `IFCloseContext.cancel()`, `IFOpenContext.cancel()`.

Titles at runtime: `open.modifyConfig { titleBuilder { primary("X") } }` (in `onOpen`), or from any context
`modifyContainer { header("New title") }` (keeps the other header components), or class-based `updateHeader(ctx, ...)`.

---

## 5. Slots, items, click handling

### 5.1 RenderContext helpers (`$IF_SURF.dsl`, file `RenderContextDSL.kt`)

```kotlin
inline fun RenderContext.slot(slot: Int, block: BukkitItemComponentBuilder.() -> Unit)               // 0-based linear
inline fun RenderContext.slot(row: Int, column: Int, block: BukkitItemComponentBuilder.() -> Unit)   // 1-based!
inline fun RenderContext.firstSlot(block) / lastSlot(block) / availableSlot(block) / resultSlot(block)
inline fun RenderContext.layoutSlot(character: Char, block)   // every slot with that layout char
```
IF members also usable directly: `slot(row, col, item: ItemStack?)`, `slot(index, item)`, `layoutSlot(char, item)`,
returning `BukkitItemComponentBuilder` for chaining (`.onClick { click -> }`).

### 5.2 Layouts (`$IF_SURF.dsl.layout`, `LayoutBuilder`)

```kotlin
inline fun ViewConfigBuilder.layout(block: LayoutBuilder.() -> Unit)
// LayoutBuilder: row(pattern) / +"XXXXXXXXX" / fill('X') / empty() / border('B')
```
Each row must be exactly 9 chars (`IllegalArgumentException`). `' '` = unnamed slot. Use inside `onInit { layout { } }`,
then `layoutSlot('X') { }` in `onFirstRender`. Provide as many rows as `settings.rows` *(IF behaviour on mismatch unverified)*.

### 5.3 BukkitItemComponentBuilder

Surf extensions (`$IF_SURF.dsl`, file `PaperItemComponentBuilderDSL.kt`):
```kotlin
inline fun BukkitItemComponentBuilder.withItem(material: Material, amount: Int = 1, init: ItemStack.() -> Unit = {})
inline fun BukkitItemComponentBuilder.withItem(type: ItemType, amount: Int = 1, init: ItemStack.() -> Unit = {})
inline fun BukkitItemComponentBuilder.renderWith(material|type, amount = 1, init)  // rebuilt every render
inline fun BukkitItemComponentBuilder.onItemRender(action: SlotRenderContext.() -> Unit)   // set `item = ...`
inline fun BukkitItemComponentBuilder.onItemClick(action: SlotClickContext.() -> Unit)
inline fun BukkitItemComponentBuilder.onItemUpdate(action: SlotContext.() -> Unit)
```
IF members: `withItem(ItemStack?)`, `renderWith(Supplier<ItemStack?>)`, `onRender{}`, `onClick{}`, `onUpdate{}`,
`cancelOnClick()`, `closeOnClick()`, `updateOnClick()`, `displayIf(BooleanSupplier|Predicate<Context>)`, `hideIf(...)`,
`watch(vararg State)`, `updateOnStateChange(State, ...)`, `identifiedBy(key)`, `withData(key, value)`.

`withItem` items are static. For items that depend on state, use `onItemRender { item = ... }` (or `renderWith`) and
call `update()` after changing the state.

`SlotClickContext` (IF) useful members: `player`, `item`, `clickedSlot`, `isLeftClick`, `isRightClick`, `isShiftClick`,
`isMiddleClick`, `isKeyboardClick`, `isOutsideClick`, `isCancelled`, `clickOrigin` (Bukkit `InventoryClickEvent`),
plus the navigation members from section 2. `SlotRenderContext`: `player`, `item` (get/set).

---

## 6. Pagination

### 6.1 `pagination`

```kotlin
// import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
context(ctx: PaginatedSurfViewContext)
inline fun <T> pagination(block: PaginationDslBuilder<T>.() -> Unit)
```
Give `T` explicitly (`pagination<Home> { }`) to avoid inference trouble.

### 6.2 `PaginationDslBuilder<T>` (`$IF_SURF.view.pagination`)

Sources (exactly one; the last call wins):

| Function | IF state | When evaluated |
|---|---|---|
| `source(items: Iterable<T>)` / `source { items }` | `buildPaginationState(list)` | **once, at view init (frame registration, i.e. server start), shared by all players** |
| `computedSource { ctx -> items }` | computed | every page load/update, per context |
| `asyncSource { ctx -> CompletableFuture<Iterable<T>> }` | computed async | every page load |
| `lazySource { ctx -> items }` | lazy | first load per context (per open) |
| `lazyAsyncSource { ctx -> future }` | lazy async | first load per context |
| `suspendSource(scope: CoroutineScope \| plugin: Plugin) { ctx -> items }` | computed async via `scope.future` | every page load |
| `lazySuspendSource(scope \| plugin) { ctx -> items }` | lazy async via `scope.future` | first load per context |

The `Plugin` overloads use MCCoroutine-Folia's `plugin.scope`. Blocking work (DB) belongs in a suspend/async source;
plain `lazySource`/`computedSource` lambdas run on the render path *(thread not further verified; on Folia assume the
player's region thread)*.

Item factory (exactly one; the last call wins):
```kotlin
fun itemFactory(factory: BukkitItemComponentBuilder.(T) -> Unit)
fun elementFactory(factory: (context: Context, builder: BukkitItemComponentBuilder, index: Int, value: T) -> Unit)
```
Optional: `onPageSwitch { ctx, pagination -> }`, `orientation(Pagination.Orientation)`, `horizontal()`, `vertical()`.

### 6.3 Generated layout and buttons

- Layout: content rows are `" " + target*7 + " "` (0-based columns 1..7), all other rows blank.
- Content rows: BOTTOM buttons → rows `(1+emptyRows) until lastRow`; TOP buttons → `2..(rows-emptyRows)` (1-based).
- Prev/next buttons are slots in the button row at 0-based columns 2 and 6, with **`ItemStack.empty()`** as the item
  and `displayIf(canBack/canAdvance)`. The visible arrows, the page counter and the "blocked" cells are drawn in the
  **title via resource-pack glyphs**; without the surf resource pack the buttons are invisible but clickable.
- `containerDefaults` of paginated views blocks (cosmetically) every non-content cell including the button row.
  Free slots for your own items: button row columns 1, 2, 4, 5, 6, 8, 9 (1-based, for `slot(row, col)`),
  and content-row columns 1 and 9.
- `context.update()` on a paginated view re-triggers `switchTo(currentPageIndex)` to refresh.

---

## 7. State

Top-level functions in `$IF_SURF.view.state` (file `StateDsl.kt`), callable only in the DSL block
(`context(ctx: AbstractSurfViewContext<*>)`). They return `StateHandle<S>`; the real IF state is created when the
view object is constructed. Registering after construction fails (`require`: "Cannot register new states after
resolution has begun").

```kotlin
fun <T> state(initialValue: T): StateHandle<State<T>>                 // immutable
fun <T> mutableState(initialValue: T): StateHandle<MutableState<T>>
fun mutableState(initialValue: Int): StateHandle<MutableIntState>     // chosen for Int literals
fun <T> computedState(computation: (Context) -> T) / computedState(() -> T): StateHandle<State<T>>  // every read
fun <T> lazyState(computation: (Context) -> T) / lazyState(() -> T): StateHandle<State<T>>          // first read per context
fun <T> initialState(): StateHandle<MutableState<T>>                  // the data passed to open(player, data)
fun <T> initialState(key: String): StateHandle<MutableState<T>>       // value from a Map<String, Any> initial data
```
Accessors (import each: `...view.state.get`, `...view.state.set`, `...view.state.increment`, `...view.state.decrement`):
```kotlin
operator fun <T> StateHandle<State<T>>.get(context: Context): T
operator fun <T> StateHandle<MutableState<T>>.get(context: Context): T
operator fun <T> StateHandle<MutableState<T>>.set(context: Context, value: T)
operator fun StateHandle<MutableIntState>.get(context: Context): Int
operator fun StateHandle<MutableIntState>.set(context: Context, value: Int)
fun StateHandle<MutableIntState>.increment(context: Context): Int
fun StateHandle<MutableIntState>.decrement(context: Context): Int
```
Values are per view context (per player session). `StateHandle.resolve()` is internal, so you **cannot** pass a
handle to IF's `watch(...)`/`updateOnStateChange(...)`; after changing a state call `update()` and render the item
with `onItemRender`/`renderWith`. `StateRegistry`/`DeferredState` are internals; ignore them.

---

## 8. Stateful buttons

`$IF_SURF.view.button` (files `ViewButtonDsl.kt`, `ViewButton.kt`, `ViewButtonHandle.kt`). Declare in the DSL block,
place in a slot with `button(handle)`. Value is per view session.

```kotlin
context(ctx: AbstractSurfViewContext<*>)
fun <T> statefulButton(initial: T, block: ViewButtonBuilder<T>.() -> Unit): ViewButtonHandle<T>
fun <T> computedStatefulButton(initial: (Context) -> T, block: ViewButtonBuilder<T>.() -> Unit): ViewButtonHandle<T>
fun toggleButton(initial: Boolean = false, block: ViewToggleButtonBuilder.() -> Unit): ViewButtonHandle<Boolean>
fun computedToggleButton(initial: (Context) -> Boolean, block: ViewToggleButtonBuilder.() -> Unit): ViewButtonHandle<Boolean>
fun <T> tripleButton(initial: T, block) / computedTripleButton(initial: (Context) -> T, block)   // require exactly 3 states

fun <T> BukkitItemComponentBuilder.button(handle: ViewButtonHandle<T>): BukkitItemComponentBuilder // cancelOnClick + render + cycle
```
- `ViewButtonBuilder<T>`: `state(value) { ... }` (declaration order = cycle order; ≥1 state required),
  `onChange { from, to -> }` (receiver `SlotClickContext`), `onCloseChanged { initial, current -> }` (receiver
  `CloseContext`, only if changed at close), `reverseOnRightClick(enabled = true)` (default on: right click goes back).
- `ViewToggleButtonBuilder`: `whenOn { }`, `whenOff { }`, `onToggle { enabled -> }`, `onCloseChanged { initial, current -> }`. Cycle fixed off→on.
- `ViewButtonStateScope<T>`: `item(material|type, amount = 1) { ItemStack init }`, `renderItem { SlotRenderContext -> ItemStack }`,
  `onEnter { from -> }`. Every state needs an appearance (`IllegalStateException` otherwise).
- `ViewButtonHandle<T>`: `handle[context]`, `handle[context] = value` (no callbacks, no repaint: call `update()`; throws
  `IllegalArgumentException` for undeclared values), `initial(context)`, `hasChanged(context)`.
- On click the slot repaints first, then `onEnter`, then `onChange` (so callbacks may navigate away).
- `initial` must be one of the declared states *(behaviour otherwise unverified)*.

---

## 9. Navigation history and "back"

Library-managed history (`ViewNavigationHistory`, internal), keyed by player UUID:
- `open(...)` from outside (viewer not switching) → history reset to `[thisView]`.
- `openForPlayer(...)` from inside a view (IF sets `isSwitching`) → push.
- Outside click with `navigateBackOnOutsideClick = true` (default) → pop and `openForPlayer(previousClass, previousData)`;
  if there is no previous entry → `closeForPlayer()`. So on a root view an outside click **closes** the menu.
- Normal close (not switching) clears history.

IF's own `back()`/`canBack()` on confined contexts resumes the previous IF *context* (no new `onOpen`). It is
independent of the surf history above. The example project's back buttons use the explicit, predictable form
`openForPlayer(parentView)`; prefer that.

---

## 10. Header / container components

Container DSL: `$IF_SURF.view.container.dsl` (file `ViewContainerDSL.kt`). Usable in `containerDefaults { }` (both
contexts available) or in `ctx.modifyContainer { }` from a lifecycle callback.

```kotlin
context(_: ViewContainerModificationContext, _: AbstractSurfViewRef)
fun header(header: Component | String, alignment: TextAlignment? = null)
fun header(alignment: TextAlignment? = null, block: SurfComponentBuilder.() -> Unit)
fun rowText(row: Int, text: Component | String, alignment = CENTER, columns: IntRange = 0..8)  // require row <= view rows
fun rowText(row: Int, alignment = CENTER, columns = 0..8, block: SurfComponentBuilder.() -> Unit)
fun rowTextAt(row: Int, column: Int, text: Component | String)                                   // left-aligned from column

context(_: ViewContainerModificationContext)
fun rowText(row, text, font: Key, alignment, columns, geometry, defaultColor, metrics)          // explicit-font variant (class-based views)
fun clearRowText(row: Int); fun clearRowTexts()
fun blockCell(column: Int, row: Int, geometry = DEFAULT)
fun blockSlot(slot: Int, geometry = DEFAULT)
fun blockRow(row: Int, exemptColumns: IntArray, geometry = DEFAULT)
fun blockRow(row: Int, exemptColumns: IntCollection = empty, geometry = DEFAULT)
fun blockColumn(column: Int, exemptRows: IntArray | IntCollection = empty, rows: Int = 6, geometry = DEFAULT)
fun blockAllSlots(rows: ViewRows | Int, exemptSlots: IntCollection = empty, geometry = DEFAULT)
fun unblockCell(column, row); fun unblockSlot(slot); fun unblockAllSlots()
fun addChild(c: ViewContainerComponent); fun removeChild(c); fun hasComponentOfType<T>(); fun removeChildrenOfType<T>()
```
- **Blocking is purely cosmetic**: it draws a texture in the title; items and clicks in that slot are unaffected.
- `rowText`, block cells, background glyph and pagination overlay all need the surf resource pack fonts
  (`surf:menu`, `surf:menu_font_row_N`). The plain title (`minecraft:default`) works without a pack.
- Pass `settings.headerGeometry` to block functions only if you changed the geometry.
- Components (`...container.component.components`): `ViewContainerTitleComponent`, `ViewContainerRowTextComponent`,
  `ViewBlockCellComponent` are public; the background glyph component is internal. Custom components implement
  `ViewContainerComponent { positionalShift; textureWidth; hasExactWidth; fun SurfComponentBuilder.renderComponent(); equals; hashCode }`
  (equal components are de-duplicated). `shift(px)` / `GlyphShift` (`$IF_SURF.view.util`) produce pixel-shift glyphs.

---

## 11. Icons and item building

### 11.1 View icons (`$IF_SURF.view.icon`)
```kotlin
fun viewIcon(icon: ViewIconType, color: ViewIconColor, init: ItemStack.() -> Unit = {}): ItemStack
class ViewIcon(icon, color) { val itemModel: String; fun build(init): ItemStack }
```
Builds a `PAPER` item, runs `init`, then sets `ITEM_MODEL` to `nexo:surf_menu_icon_<color>_<type>` (needs the pack).
- `ViewIconType`: ARROW_DOWN, ARROW_UP, ARROW_LEFT, ARROW_RIGHT, CROSS, CHECK, USERS, HOME, COG, BELL, PLUS, MINUS,
  EQUALS, MENU, ARROW_PREVIOUS, ARROW_NEXT, RELOAD, QUESTION_MARK, EXCLAMATION_MARK, QUESTION, SEARCH, CIRCLE.
- `ViewIconColor`: RED, BLUE, WHITE, GREEN, YELLOW.

### 11.2 Item builder (`dev.slne.surf.api.paper.builder`)
```kotlin
inline fun buildItem(material: Material, amount: Int = 1, init: ItemStack.() -> Unit): ItemStack
inline fun buildItem(type: ItemType, amount: Int = 1, init: ItemStack.() -> Unit): ItemStack
inline fun <reified M : ItemMeta> ItemStack.meta(block: M.() -> Unit): Boolean
fun ItemStack.displayName(name: Component)                       // italic off unless set
inline fun ItemStack.displayName(block: SurfComponentBuilder.() -> Unit)
fun ItemStack.lore(vararg lore: Component)
fun ItemStack.lore(block: SurfComponentBuilder.() -> Unit)       // ONE lore line
fun ItemStack.buildLore(block: LoreBuilder.() -> Unit)           // multiple lines
class LoreBuilder { fun line(block: SurfComponentBuilder.() -> Unit); operator fun Component.unaryPlus(); fun emptyLine() }
```
`buildItem` has no default for `init`; pass `{}` if you have nothing to set.

### 11.3 SurfComponentBuilder (`dev.slne.surf.api.core.messages.builder.SurfComponentBuilder`)
Colour helpers (each overloaded for `String`, `Boolean`, `Char`, `Number`, plus `vararg TextDecoration`):
`primary`, `secondary`, `info`, `success`, `warning`, `error`, `variableKey`, `variableValue`, `spacer`,
`darkSpacer`, and Minecraft colours `black`, `darkBlue`, `darkGreen`, `darkAqua`, `darkRed`, `darkPurple`, `gold`,
`gray`, `darkGray`, `blue`, `green`, `aqua`, `red`, `lightPurple`, `yellow`, `white`. Also `text(value, color?, vararg deco)`,
`append(...)`, `appendSpace()`, `appendNewline()`, `color(..)`, `decorate(..)`, `font(..)`, `build()`.
Colours: `dev.slne.surf.api.core.messages.Colors.PRIMARY` etc. Helpers: `dev.slne.surf.api.core.messages.adventure.buildText { }`,
`...adventure.text(content, color)`, `dev.slne.surf.api.core.font.toSmallCaps()`, and
`dev.slne.surf.api.core.messages.adventure.playSound(useSelfEmitter) { type(Sound.X) }` on any `Audience`.

---

## 12. Import cheat sheet

```kotlin
import dev.slne.surf.api.paper.inventory.framework.open                 // View.open(player[, data])
import dev.slne.surf.api.paper.inventory.framework.register              // View.register()
import dev.slne.surf.api.paper.inventory.framework.modifyConfig          // OpenContext.modifyConfig { }
import dev.slne.surf.api.paper.inventory.framework.titleBuilder
import dev.slne.surf.api.paper.inventory.framework.view.*                // surfView, paginatedSurfView, settings, onFirstRender,
                                                                        // onOpen, onClick, onClose, onUpdate, onInit,
                                                                        // containerDefaults, layoutTarget, modifyContainer, view,
                                                                        // AbstractSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.settings.{ViewRows, PaginationViewRows, PaginationEmptyRows, PaginationButtonPosition}
import dev.slne.surf.api.paper.inventory.framework.view.state.{mutableState, state, lazyState, computedState, initialState, get, set, increment, decrement}
import dev.slne.surf.api.paper.inventory.framework.view.button.{toggleButton, computedToggleButton, statefulButton, tripleButton, button}
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.{blockRow, blockCell, rowText, header}
import dev.slne.surf.api.paper.inventory.framework.view.icon.{viewIcon, ViewIconType, ViewIconColor}
import dev.slne.surf.api.paper.inventory.framework.dsl.{slot, layoutSlot, layout, withItem, renderWith, onItemClick, onItemRender, onItemUpdate, openForPlayer, cancel}
import dev.slne.surf.api.paper.builder.{buildItem, displayName, buildLore, lore}
import me.devnatan.inventoryframework.context.{Context, RenderContext, SlotClickContext, OpenContext, CloseContext}
```
(Kotlin has no brace imports; write one line per symbol. Note `settings` the function lives in `...view`, while the
settings *types* live in `...view.settings`; a star import of `...view.*` brings in the function.)

---

## 13. Project-local helpers (NOT library API)

The example project defines these in its own `view-utils.kt`. Other projects must copy them if wanted:

```kotlin
import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.view.icon.*
import me.devnatan.inventoryframework.context.Context
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Sound
import org.bukkit.entity.Player

fun Context.playGeneralClickSound() { player.playSound(true) { type(Sound.UI_BUTTON_CLICK) } }
fun Context.playNoSound() = player.playNoSound()
fun Player.playNoSound() { playSound(true) { type(Sound.ENTITY_VILLAGER_NO) } }
fun Context.playYesSound() = player.playYesSound()
fun Player.playYesSound() { playSound(true) { type(Sound.ENTITY_VILLAGER_YES) } }

val backItem = viewIcon(ViewIconType.CROSS, ViewIconColor.RED) {
    displayName { primary("Zurück", TextDecoration.BOLD) }
    buildLore { emptyLine(); line { spacer("Zum vorherigen Menü zurückkehren".toSmallCaps()) }; emptyLine() }
}
val closeItem = viewIcon(ViewIconType.CROSS, ViewIconColor.RED) {
    displayName { primary("Schließen", TextDecoration.BOLD) }
    buildLore { emptyLine(); line { spacer("Das Menü schließen".toSmallCaps()) }; emptyLine() }
}
```
Recommended style seen in the example project: top-level `val xView: AbstractSurfView = surfView(...)`, item stacks
as private top-level `val`s built with `viewIcon` + `displayName` + `buildLore` (empty line, small-caps `spacer`
line, empty line), a click sound before every action, `navigateBackOnOutsideClick(false)` on root menus,
`blockRow(...)` in `containerDefaults` for the framed look.

---

## 14. Worked examples

The examples use the project-local helpers from section 13 (`playGeneralClickSound`, `backItem`, `closeItem`);
copy them or replace them. `warpService` and `soundSettings` are placeholders.

### 14.1 Static menu navigating to a second view

```kotlin
package com.example.menu

import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.openForPlayer
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.view.AbstractSurfView
import dev.slne.surf.api.paper.inventory.framework.view.containerDefaults
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.surfView
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.blockRow
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.paper.inventory.framework.view.icon.viewIcon
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewRows

// Explicit types: the two vals reference each other (see Gotchas).
val mainMenu: AbstractSurfView = surfView("Menü") {
    settings {
        rows(ViewRows.THREE)
        navigateBackOnOutsideClick(false)      // root menu: outside click does nothing
    }
    containerDefaults { blockRow(1); blockRow(3) }

    onFirstRender {
        slot(2, 5) {                           // row 2, column 5 (1-based) = centre
            withItem(settingsIcon)
            onItemClick {
                playGeneralClickSound()
                openForPlayer(settingsMenu)    // pushes history
            }
        }
        slot(3, 9) {
            withItem(closeItem)
            onItemClick { closeForPlayer() }
        }
    }
}

val settingsMenu: AbstractSurfView = surfView("Einstellungen") {
    settings { rows(ViewRows.THREE) }          // outside click goes back (default)
    onFirstRender {
        slot(3, 1) {
            withItem(backItem)
            onItemClick { playGeneralClickSound(); openForPlayer(mainMenu) }
        }
    }
}

private val settingsIcon = viewIcon(ViewIconType.COG, ViewIconColor.WHITE) {
    displayName { primary("Einstellungen") }
    buildLore { emptyLine(); line { spacer("Öffnet die Einstellungen") }; emptyLine() }
}

// In JavaPlugin#onLoad:  mainMenu.register(); settingsMenu.register()
// To open:               mainMenu.open(player)      (import ...inventory.framework.open)
```

### 14.2 Paginated list with lazy source and back button

```kotlin
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.openForPlayer
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.view.layoutTarget
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import org.bukkit.Material

data class Warp(val name: String, val icon: Material)

val warpListView: AbstractPaginatedSurfView = paginatedSurfView("Warps") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)   // 3 content rows + button row = 4 rows
        paginationPageIndicator { cur, total -> if (total <= 1) null else net.kyori.adventure.text.Component.text("$cur/$total") }
    }

    layoutTarget('W')

    pagination<Warp> {
        // Evaluated once per opened context; player-specific data is fine here.
        lazySource { ctx -> warpService.visibleWarps(ctx.player) }
        itemFactory { warp ->
            withItem(buildItem(warp.icon) { displayName { primary(warp.name) } })
            onItemClick {
                playGeneralClickSound()
                closeForPlayer()
                player.performCommand("warp ${warp.name}")
            }
        }
    }

    onFirstRender {
        // Button row is row 4. Columns 3 and 7 (1-based) are the page arrows; use column 1 for "back".
        slot(4, 1) {
            withItem(backItem)
            onItemClick { playGeneralClickSound(); openForPlayer(mainMenu) }
        }
    }
}
```
For a DB-backed list use `lazySuspendSource(plugin) { ctx -> repository.load(ctx.player.uniqueId) }`.

### 14.3 State: counter and toggle

```kotlin
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemRender
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.view.AbstractSurfView
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.surfView
import dev.slne.surf.api.paper.inventory.framework.view.button.button
import dev.slne.surf.api.paper.inventory.framework.view.button.computedToggleButton
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewRows
import dev.slne.surf.api.paper.inventory.framework.view.state.get
import dev.slne.surf.api.paper.inventory.framework.view.state.increment
import dev.slne.surf.api.paper.inventory.framework.view.state.mutableState
import org.bukkit.Material

val counterView: AbstractSurfView = surfView("Zähler") {
    settings { rows(ViewRows.ONE) }

    val clicks = mutableState(0)          // StateHandle<MutableIntState>, per player session

    val sound = computedToggleButton({ ctx -> soundSettings.isEnabled(ctx.player) }) {
        whenOn  { item(Material.NOTE_BLOCK) { displayName { success("Sound: an") } } }
        whenOff { item(Material.BARRIER)    { displayName { error("Sound: aus") } } }
        onCloseChanged { _, current -> soundSettings.setEnabled(player, current) }   // persist once, on close
    }

    onFirstRender {
        slot(1, 3) {
            onItemRender {                // re-evaluated on every update()
                item = buildItem(Material.PAPER, amount = clicks[this].coerceIn(1, 64)) {
                    displayName { primary("Klicks: "); variableValue(clicks[this@onItemRender]) }
                }
            }
            onItemClick {
                clicks.increment(this)
                update()                  // re-render the context so onItemRender runs again
            }
        }
        slot(1, 7) { button(sound) }      // cancels clicks, repaints itself
    }
}
```

---

## 15. Gotchas

1. **Recursive type inference.** Two top-level vals that reference each other (e.g. `openForPlayer(otherView)` or
   `otherView::class.java` inside their DSL lambdas) with inferred types fail with
   *"Type checking has run into a recursive problem"*. Fix: declare the type explicitly:
   `val a: AbstractSurfView = surfView(...) { ... }` and
   `val b: AbstractPaginatedSurfView = paginatedSurfView(...) { ... }`
   (`dev.slne.surf.api.paper.inventory.framework.view.AbstractSurfView`,
   `dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView`).
2. `slot(row, column)` is **1-based**; `slot(index)` is 0-based; container block/rowText use 1-based rows but
   0-based columns, and `blockCell` takes `(column, row)`.
3. Register views in `onLoad`, before the shared ViewFrame registers; late `register()` leaves the view uninitialised.
   Every view you open (including via `openForPlayer`) must be registered.
4. Don't create views per player or per call: the DSL block, `onInit` and static `source {}` run once. Build each view
   once as a top-level `val`/singleton. Calling one `surfView` call site twice produces two instances of the same
   class; IF's class lookup would then find only one.
5. `source { }` is evaluated at server start and shared; use `lazySource`/`computedSource`/suspend sources for
   per-player or changing data.
6. `onFirstRender` runs once per open. `withItem` is static; dynamic items need `onItemRender`/`renderWith` + `update()`.
7. Default `navigateBackOnOutsideClick = true`: an outside click on a menu opened via `open(player)` **closes** it, and on
   child menus it reopens the parent with its original data (re-running `onOpen`/`onFirstRender`).
8. All cancel flags default to `true`; items cannot be taken out unless you disable them.
9. Pagination arrows, page counter, block cells, row texts, background glyph and `viewIcon` models require the surf
   resource pack (Nexo item models `nexo:surf_menu_icon_*`). Without it the arrows are invisible (empty items).
10. Paginated views: your `onInit { layout {} }` is overwritten; only 7 items per row; size comes from
    `paginationViewRows`, not `rows`.
11. Calling a hook twice (e.g. two `onFirstRender`) silently keeps only the last.
12. `mutableState(0)` yields a `MutableIntState` handle; import the `get`/`set`/`increment` extension operators
    explicitly or `clicks[ctx]` will not compile.
13. Setting a title with `onInit { title(...) }` or `open.modifyConfig { title(...) }` is adopted as the header;
    `context.updateTitleForPlayer(...)` (IF) bypasses the container and drops header glyphs. Use
    `modifyContainer { header(...) }` instead.
14. `ViewButtonHandle[ctx] = v` does not fire callbacks or repaint; call `update()`.
15. On Folia, open views/mutate contexts on the player's thread (e.g. from a coroutine use the entity dispatcher);
    the library itself schedules overlay updates via `player.scheduler`. *(General guidance, not enforced by checks.)*
