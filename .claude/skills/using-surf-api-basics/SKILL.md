---
name: using-surf-api-basics
description: Use when writing Kotlin code in a project that depends on surf-api and the player sees or the code needs a basic helper - chat messages, action bar, titles, sounds, hover/click text, colors, prefixes, name/UUID lookup, config files, components/services, glowing, small-caps text, requiredService, forEachPlayer, NamespacedKey. Also when about to write Component.text, NamedTextColor or MiniMessage.
---

# surf-api basics

## Overview

surf-api ships helpers for everything a player reads and for common plumbing. Use them before
reaching for raw Adventure, Bukkit or hand-rolled code. Source: docs.slne.dev/surf-api (Grundlagen,
Erweiterte Funktionen). Menus and dialogs have their own skills: `building-surf-inventory-guis`,
`building-surf-dialogs`.

**Never** build player text with `Component.text(..., NamedTextColor.X)`, `MiniMessage` or legacy codes.

## Text: `buildText` / `sendText`

```kotlin
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText

player.sendText {
    appendPrefix()
    success("Du wurdest ")
    variableValue("zur Whitelist")
    success(" hinzugefügt.")
    hoverEvent(buildText { info("Klicke zum Entfernen.") })
    clickRunsCommand("/whitelist remove $name")
}
val component = buildText { primary("Titel"); spacer(" - "); secondary("Untertitel") }
```

On a Paper `Player`, `sendActionBar { }` resolves to the wrong overload. Write
`player.sendActionBar(buildText { ... })`.

### Color = meaning (`farbdesign`)

| Helper | Use for | Hex |
|---|---|---|
| `primary` | titles, headings | #3b92d1 |
| `secondary` | subtitles, captions | #5b5b5b |
| `info` | information not tied to a user action | #40d1db |
| `success` | result of a successful user action | #65ff64 |
| `warning` | possible problem, usually before an error | #f9c353 |
| `error` | error messages; send them with no other colors mixed in | #ee3d51 |
| `variableKey` / `variableValue` | key / value in a listing | info / warning |
| `spacer` | `-`, `...`, `/` | #AAAAAA |
| `darkSpacer` | `>>`, `\|` as in the prefix | #555555 |

User wishes map to helpers: "blue" = `primary`, "green" = `success`, "gray" = `spacer`.

### Extra builder functions

`appendPrefix()`, `appendSpace()`, `appendNewLine()`, `appendNewPrefixedLine()`, `append(component)`,
`text(...)`, `content(str)` (uncolored), `ellipsis()`, `decorate(..)`, `decorationIfAbsent(..)`,
`font(key)`, `style(..)`, `shadowColor(argb)`, `hoverEvent(component)`, `clickEvent(..)`,
`clickRunsCommand(cmd)`, `appendDiscordLink()`, `appendDisconnectFooter()`,
`appendDisconnectFooterTryAgainLater(isIssue)`.

`sendText` works on any `Audience`, including `CommandSender` (console safe). The helpers accept
`Any`, so `variableValue(level)` works with an `Int`. Put `appendPrefix()` first, also before `error(..)`.
`hoverEvent` / `clickRunsCommand` are called on the builder; the docs do not say which span they cover,
so check a nearby usage before relying on partial-span events.

## Sound and title

```kotlin
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.showTitle
import org.bukkit.Sound                        // type(..) takes the Bukkit enum, e.g. UI_BUTTON_CLICK
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

player.playSound {                       // playSound(true) { } uses the self emitter
    type(Sound.BLOCK_COMPARATOR_CLICK)
    source(net.kyori.adventure.sound.Sound.Source.BLOCK)
    pitch(1f); volume(1f)
}
player.showTitle {
    title { primary("Titel") }
    subtitle { spacer("Untertitel") }
    times { fadeIn(250.milliseconds); stay(10.seconds); fadeOut(250.milliseconds) }
}
```

## Quick reference: other helpers

| Need | Use | Import |
|---|---|---|
| Name from UUID / UUID from name | `PlayerLookupService.getUsername(uuid)` / `.getUuid(name)` (nullable, **async only**, cached 15 min) | `dev.slne.surf.api.core.service.PlayerLookupService` |
| Find a service | `requiredService<T>()` | `dev.slne.surf.api.core.util.requiredService` |
| Small caps text | `"text".toSmallCaps()` | `dev.slne.surf.api.core.font.toSmallCaps` |
| NamespacedKey of the plugin | `key("name")` | `dev.slne.surf.api.core.messages.adventure.key` |
| All online players | `forEachPlayer { }` | `dev.slne.surf.api.paper.util.forEachPlayer` |
| Register a listener | `listener.register()` | `dev.slne.surf.api.paper.event.register` |
| Global SecureRandom | `random` | `dev.slne.surf.api.core.util.random` |
| FastUtil collections | `mutableObjectListOf`, `object2IntMapOf`, ... | `dev.slne.surf.api.core.util.*` |
| Distance, chunk coords, vector | `Location.distance/distanceSquared/chunkX/chunkZ/toVector3d()` | surf-api extensions |
| Date formatting (EU) | `dateTimeFormatter`, `currentDateTimeFormatted` | surf-api extensions |

The docs show `getUsername(name)` for the reverse lookup. That is a typo; real code uses `getUuid(name)`.
`PlayerLookupService` is an object with `suspend` functions: call it directly (no `requiredService`) from
a coroutine, e.g. `plugin.launch { PlayerLookupService.getUuid(name) }`, never on the main thread.

## Config files (`surfConfigApi`)

```kotlin
import dev.slne.surf.api.core.config.surfConfigApi

val config = surfConfigApi.createSpongeYmlConfig<MyConfig>(folder, "config.yml")  // or ...JsonConfig, createDazzlConfig
surfConfigApi.getSpongeConfig<MyConfig>()      // cached instance
surfConfigApi.reloadSpongeConfig<MyConfig>()   // re-read from disk
```

Dazzl twins: `createDazzlConfig`, `getDazzlConfig`, `reloadDazzlConfig` (YAML only).
`getSpongeConfigManagerForConfig(MyConfig::class.java)` returns the manager for saving.

## Component system (lifecycle classes)

`@SurfComponentMeta class X : AbstractComponent()` is found at compile time. Do not register it by hand.
Override `onLoad` / `onEnable` / `onDisable` (suspend). Disable runs in reverse init order.
Meta-annotations: `@Service`, `@Priority(n)` (lower first, default 0),
`@ConditionalOnProperty(key = [...], havingValue = ...)`, `@ConditionalOnEnvironment(environments = [...])`
(`SURF_ENVIRONMENT` in `.env`). The plugin must call `surfComponentApi.load(this)` / `enable(this)` /
`disable(this)` in `onLoadAsync` / `onEnableAsync` / `onDisableAsync` of a `SuspendingJavaPlugin`.
Implementing `SurfComponent` directly gives no phase chaining; prefer `AbstractComponent`.

## Glowing (Paper only)

`glowingApi.makeGlowing(target, viewer, NamedTextColor.RED)` / `removeGlowing(target, viewer)`.
Target: entity, block or location. Entities take an optional color, blocks and locations require one.

## Common mistakes

| Mistake | Fix |
|---|---|
| `Component.text("..", NamedTextColor.GREEN)` | `buildText { success("..") }` |
| Message without prefix | start with `appendPrefix()` |
| Error message in several colors | one `error(..)` span |
| `sendActionBar { }` lambda | `sendActionBar(buildText { })` |
| `PlayerLookupService` on the main thread | call from a coroutine / async task |
| Manually registering an `AbstractComponent` | annotate with `@SurfComponentMeta` only |
| Guessing an import | copy it from the table above or grep the repo |

Docs index: https://docs.slne.dev/llms.txt. Scoreboard Framework and Inventory/Gui pages are covered elsewhere or empty.
