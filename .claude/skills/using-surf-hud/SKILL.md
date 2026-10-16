---
name: using-surf-hud
description: Use when showing persistent on-screen text to players in a Kotlin Paper project that depends on surf-hud (dev.slne.surf.hud) - HUD lines above the hotbar, region or status displays, player.hud, HudElement, HudBuilder, "hud.add", "hud.content", or when about to use a boss bar or action bar for permanent information.
---

# surf-hud

## Overview

surf-hud draws a small HUD per player from boss bars and a resource-pack font. Each player has one `Hud`
that holds `HudElement`s. Use it for information that stays on screen; keep one-off messages in chat, the
action bar or titles (see `using-surf-api-basics`).

## Setup

```kotlin
// build.gradle.kts
repositories { maven("https://reposilite.slne.dev/releases") }
dependencies { compileOnly("dev.slne.surf.hud:surf-hud-api:1.0.1") }
surfPaperPluginApi { serverDependencies { registerRequired("surf-hud-paper") } }
```

The server needs the `surf-hud-paper` plugin and the resource pack with the fonts `surf:hud`, `surf:hud2`
and `surf:space`. Without the pack players see garbage glyphs.

## Model

| Type | Meaning |
|---|---|
| `Hud` | The HUD of one player: `player.hud` (import `dev.slne.surf.hud.api.hud`) |
| `HudElement(id, line, position, content, background = true)` | One piece of text. `id` must not be blank, `line >= 0` |
| `line` | Row; the default theme has 2 (0 and 1) |
| `position` | `0` centers the element; negative values sit left, positive right, in order |

## Use

```kotlin
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.hud.api.HudElement
import dev.slne.surf.hud.api.hud

player.hud.add(HudElement("region", 0, 0, buildText { primary("Mining") }))  // add or replace by id
player.hud.content("region", buildText { primary("Fishing") })              // change text, false if id is unknown
player.hud.remove("region")                                                  // remove one element
```

Several elements at once: `player.hud.add { line(0) { center("a", c1); element("b", 1, c2) } }`.
`visible` hides the whole HUD, `refresh()` forces a redraw.

The released 1.0.1 artifact names the method `add`. Newer source on `main` may differ, so check the
artifact (`javap` on the jar) before copying code from the repository.

## Rules

- **Own your element ids.** Add, change and remove only the ids your feature owns. Never call `clear()` or
  `add { }` for the whole HUD, it wipes the elements of other features. Current ids: `region`.
- **Build text with surf-api helpers** (`buildText`), not raw `Component.text`.
- **Update only on change.** Compare with what you showed before; the HUD already skips identical content,
  but computing it every tick is wasted work.
- **Folia:** read player state on the player's own scheduler (`player.scheduler.runAtFixedRate`), not on a
  global task.
- **Quit:** surf-hud drops the HUD on quit; stop your own tasks and caches for the player as well.
- **Join:** a HUD you did not set after the player joined is empty; set your elements on join and for
  players who are already online when your plugin enables.

## Common mistakes

| Mistake | Fix |
|---|---|
| Glyph garbage instead of the HUD | The resource pack is missing on the client |
| `content()` does nothing | The id was never added; call `add` first |
| `IllegalArgumentException` for a line | The theme only has 2 lines (0 and 1) |
| Plugin does not start | `surf-hud-paper` is not installed on the server |
| `Unresolved reference 'set'` | 1.0.1 uses `add`, not `set` |

## Example in this repository

`RegionHud` in `skillworld/hud/` shows the region of a player as element `region` in line 0, updated by a
10-tick task on the player's scheduler.
