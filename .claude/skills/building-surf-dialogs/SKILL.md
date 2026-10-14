---
name: building-surf-dialogs
description: Use when creating, changing or debugging a Minecraft dialog (Paper 1.21.6+ Dialog API) in a Kotlin project that depends on surf-api (dev.slne.surf.api.paper.dialog) - forms, input prompts, confirmations, notices, button menus, paginated or search dialogs, or validating player input from a dialog (DialogResponseView, customPlayerClick, inputDialog, DialogSchema).
---

# Building dialogs with surf-api

## Overview

surf-api wraps Paper's dialog API in a Kotlin DSL: `dialog { base { … } ; type { … } }`.
For any dialog that **collects input**, use the zod-style schema layer (`DialogSchema` +
`inputDialog`). You declare fields and rules once, and get typed values plus automatic
"reopen with errors" handling. Don't hand-roll `response.getText(...)` and a separate validation or error dialog.

## Pick the tool

| Need | Use | Read |
|---|---|---|
| Form or prompt with validated inputs | `object X : DialogSchema()` + `inputDialog(X) { … }` | `schema-reference.md` |
| Info message, one OK button | `noticeDialog(title, message)` or `dialog { base{}; type { notice {…} } }` | `surf-api-dialog-reference.md` §2, §6 |
| Yes/no without inputs | `type { confirmation { yes {…}; no {…} } }` | §6, §14.2 |
| Button menu | `type { multiAction { action {…} } }` | §6, §14.3 |
| Paged list of entries | `paginatedDialog(player, query, …)` (suspend) | §11 |
| Single search field | `searchDialog(…)` | §12 |
| Re-rendering stateful dialog | `composableDialog(player, state, scope) { … }` | §10 |

Both reference files are next to this SKILL.md. Read the relevant section before writing code;
signatures and imports are there.

## Setup check (do this first)

1. Confirm the module depends on surf-api paper: `dev.slne.surf.api.paper.dialog` must be importable. Usually that comes from the `dev.slne.surf.api.gradle.paper-*` Gradle plugin.
2. For input dialogs, search the project for `abstract class DialogSchema`. If it is missing, copy `schema/*.kt` from this skill into one package of the module and set the `package` line (see `schema-reference.md` → "Getting the code").
3. Find how the project launches coroutines, usually MCCoroutine `plugin.launch { }`, and use it for suspend work in callbacks.

## Core pattern

```kotlin
object RenameSchema : DialogSchema() {
    val name by string("Name") {
        trim()
        nonEmpty("Der Name darf nicht leer sein.")
        max(16, "Maximal 16 Zeichen.")
    }
}

fun renameDialog(current: String?): Dialog = inputDialog(RenameSchema) {   // fresh per show
    title { primary("Umbenennen") }
    initial(RenameSchema.name, current)
    submit("Speichern") { player, values ->
        val name = values[RenameSchema.name]              // typed String, already validated
        plugin.launch { repository.rename(player.uniqueId, name) }
    }
    cancel("Abbrechen") { player -> player.closeDialog() }
}

player.showDialog(renameDialog(old))
```

## Rules that bite

- **Build a dialog per show.** Click callbacks default to **one use** with a 12h lifetime, and they are registered when the dialog is built. A dialog cached in a top-level `val` has dead buttons after the first click.
- **Only `customClick`/`customPlayerClick` (and `commandTemplate`) can read inputs.** `callback` and `playerCallback` can't see them.
- **Callbacks are not suspend.** Launch a coroutine for IO, and copy the response values into locals first. Never block inside a callback.
- **Import `dev.slne.surf.api.paper.dialog.base` and `.type`.** Without those imports, `base {}` resolves to Paper's `base(DialogBase)` and fails to compile.
- **`title { … }` takes a component lambda.** Put `body {}` and `input {}` next to it as siblings, never inside it. Nested, they still compile through the outer receiver, but the code is misleading.
- **Every input, button and title needs a label.** The builders `require(...)` it at build time and throw `IllegalArgumentException` when `dialog {}` runs.
- `base {}` always sets `pause(false)`. Files that build dialogs need `@file:Suppress("UnstableApiUsage")`.
- Text the player sees follows the project's language. In existing surf projects that is German, e.g. "Abbrechen", "Zurück", "Speichern".

## Switching between dialogs and inventory menus

- **Dialog → menu** (cancel/back buttons, after submit): call `menuView.open(player)` (import `dev.slne.surf.api.paper.inventory.framework.open`). This resets the menu's navigation history.
- **Menu → dialog:** in the click handler, call `closeForPlayer()` and then `player.showDialog(...)`.
- The menu side is covered by the **`building-surf-inventory-guis`** skill.

## Common mistakes

| Mistake | Fix |
|---|---|
| A separate `XErrorDialog` plus an `InputState` data class for validation | Use a `DialogSchema` and let `inputDialog` reopen the dialog with the errors |
| `nonEmpty` before `trim()` | Put `trim()` first, because rules run in order |
| `val myDialog = dialog { … }` shown repeatedly | `fun myDialog() = dialog { … }` |
| Suspend or database call directly inside `submit` or `customPlayerClick` | `plugin.launch { … }` |
| `paginatedDialog` called from non-suspend code | Call it inside a coroutine; it needs a `scope` |
