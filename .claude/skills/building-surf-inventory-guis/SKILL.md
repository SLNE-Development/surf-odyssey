---
name: building-surf-inventory-guis
description: Use when creating, changing or debugging a Minecraft chest/inventory GUI or menu in a Kotlin Paper project that depends on surf-api (dev.slne.surf.api.paper.inventory.framework, devnatan inventory-framework) - surfView, paginatedSurfView, slots, click handlers, pagination, state, toggle buttons, menu navigation, viewIcon items, "View not registered", or "Type checking has run into a recursive problem" between views.
---

# Building inventory GUIs with surf-api

## Overview

surf-api wraps devnatan's inventory-framework (IF) in a Kotlin DSL. Each menu is built
**once** as a top-level `val` (`surfView` / `paginatedSurfView`), **registered in `onLoad`**,
and opened per player with `view.open(player)`. All per-player behaviour lives in the
lifecycle callbacks (`onFirstRender`, `onItemClick`, `onItemRender`, state).

The full API is in `reference.md`, next to this file. Read the matching section before coding:
the DSL functions are top-level functions with context parameters, so each one needs an
**explicit import**, and the reference lists them (§12).

## Pick the tool

| Need | Use | Reference |
|---|---|---|
| Fixed menu with buttons | `surfView("Titel") { settings { rows(ViewRows.THREE) }; onFirstRender { slot(r, c) { … } } }` | §1.1, §3.1, §5 |
| List of entries, paged | `paginatedSurfView` + `layoutTarget('X')` + `pagination<T> { lazySource {…}; itemFactory {…} }` | §1.2, §6 |
| Per-player or changing data | `lazySource` / `computedSource` / `lazySuspendSource(plugin)`, **not** `source {}` | §6.2 |
| Counter, toggle, live item | `mutableState`, `computedToggleButton`, `onItemRender` + `update()` | §7, §8 |
| Navigate between menus | `openForPlayer(otherView)`, `back()`, `navigateBackOnOutsideClick` | §2, §9 |
| Framed look, header text | `containerDefaults { blockRow(1) }`, `modifyContainer { header(…) }` | §10 |
| Item icons | `viewIcon(ViewIconType.X, ViewIconColor.Y) { displayName {…}; buildLore {…} }`, `buildItem(...)` | §11 |
| Full control or inheritance | `AbstractSurfView` / `AbstractPaginatedSurfView` subclasses | §1.3 |

## Setup check (do this first)

1. Find where the plugin registers views. Search for `.register()` on views and for `onLoad`.
   Every view you create or open, including targets of `openForPlayer`, must be registered in `JavaPlugin#onLoad`. A late `register()` leaves the view uninitialised, and opening an unregistered view throws "View not registered".
   If the project has no registration spot yet, add one and say so.
   If `onLoad` is `final`, find the load hook the plugin base exposes, such as an `onLoaded()`/`onLoadAsync()` hook of an MCCoroutine `SuspendingJavaPlugin`, and register before any suspending call there.
   It is not verified whether that hook runs early enough. After registering there, open the view once in-game to confirm it works, and tell the user about the risk.
2. Look for project helpers such as click sounds and back/close icons, for example a `view-utils.kt` with `playGeneralClickSound()`, `backItem` and `closeItem`. Reuse them. If none exist, `reference.md` §13 has a version you can copy.
3. Copy the style of existing menus in the project: titles, German labels, lore layout.

## Core pattern

```kotlin
val shopView: AbstractSurfView = surfView("Shop") {          // explicit type, see Rules
    settings {
        rows(ViewRows.THREE)
        navigateBackOnOutsideClick(false)
    }
    containerDefaults { blockRow(1); blockRow(3) }

    onFirstRender {
        slot(2, 5) {                                            // row 2, column 5 — 1-based
            withItem(shopIcon)
            onItemClick {
                playGeneralClickSound()
                openForPlayer(shopListView)                     // must be registered too
            }
        }
    }
}

private val shopIcon = viewIcon(ViewIconType.HOME, ViewIconColor.WHITE) {
    displayName { success("Shop") }
    buildLore { emptyLine(); line { spacer("Alle Angebote".toSmallCaps()) }; emptyLine() }
}

// onLoad:   shopView.register(); shopListView.register()
// anywhere: shopView.open(player)
```

## Rules that bite

- **Give views explicit types.** Write `val x: AbstractSurfView = surfView(...)` and `val y: AbstractPaginatedSurfView = paginatedSurfView(...)`. Two views that reference each other with inferred types fail with "Type checking has run into a recursive problem".
- **Mind the coordinate bases.** `slot(row, column)` is 1-based, even though the library KDoc says zero-based. `slot(index)` is 0-based. Container `blockCell(column, row)` takes the column first, with 0-based columns and 1-based rows. See §0.
- **Build each view once.** The DSL block and `source {}` run once at startup and are shared by all players. Never call `surfView` per player or per click.
- **Content that changes needs `onItemRender` plus `update()`.** `withItem` is static. Setting state or a button value doesn't repaint until you call `update()`.
- **Paginated views have a fixed layout.** They hold 7 items per row. `paginationViewRows` decides the size, and the last row holds the page arrows at columns 3 and 7, so put "back" at column 1 or 9. They require `layoutTarget` and `pagination`, and throw at construction without them.
- **Outside clicks navigate back by default.** `navigateBackOnOutsideClick` defaults to true: on root menus an outside click closes the menu. Set it to `false` there if you don't want that.
- **Each hook is a single slot.** A second `onFirstRender` replaces the first one.
- Arrows, icons and block glyphs need the surf resource pack. Without it, they show as empty items.
- On Folia, open views from the player's thread, and hop back there from coroutines.
- Text the player sees follows the project's language, which is German in existing surf projects.

## Switching between menus and dialogs

- **Menu → dialog:** in the click handler, call `closeForPlayer()` and then `player.showDialog(buildDialog())`. Build the dialog fresh for each show. Closing first is the safe pattern; showing a dialog over an open inventory hasn't been tested.
- **Dialog → menu:** call `view.open(player)` from the dialog callback. This **resets** IF's navigation history, so `back()` won't return to earlier menus.
- On Folia, these callbacks run on the player's thread. Inside a coroutine, hop back to the player's thread before opening anything.
- For the dialog itself, **REQUIRED SUB-SKILL:** `building-surf-dialogs`.

## Common mistakes

| Mistake | Fix |
|---|---|
| `slot(0, 0)` used as the top-left slot | Use `slot(1, 1)`. 0 is clamped, so off-by-one bugs stay hidden. |
| `source { repo.load(player) }` | Use `lazySource { ctx -> … }` or `lazySuspendSource(plugin) { ctx -> … }` |
| A new view only created, never registered | Add `register()` in `onLoad` |
| `clicks[ctx]` doesn't compile | Import the `state.get`, `set` and `increment` extensions |
| `context.updateTitleForPlayer(...)` drops the header glyphs | Use `modifyContainer { header(...) }` |
| A form or prompt built as a chest GUI | Use a dialog instead. **REQUIRED SUB-SKILL:** `building-surf-dialogs` |
