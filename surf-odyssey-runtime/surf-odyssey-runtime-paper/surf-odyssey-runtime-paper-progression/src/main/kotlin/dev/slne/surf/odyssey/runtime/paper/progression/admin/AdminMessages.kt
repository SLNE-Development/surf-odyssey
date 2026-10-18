package dev.slne.surf.odyssey.runtime.paper.progression.admin

import java.nio.file.Path

/** The texts the admin commands send. */
object AdminMessages {
    /** Names a quest ID that no quest has. */
    fun unknownQuest(id: String): String = "Unbekannte Quest: $id"

    /** Names a recipe ID that no recipe has. */
    fun unknownRecipe(id: String): String = "Unbekanntes Rezept: $id"

    /** Names a title ID that no title has. */
    fun unknownTitle(id: String): String = "Unbekannter Titel: $id"

    /** Names an item ID that no custom item has. */
    fun unknownItem(id: String): String = "Unbekanntes Item: $id"

    /** Names a tool ID that no tool has. */
    fun unknownTool(id: String): String = "Unbekanntes Werkzeug: $id"

    /** Names an NPC ID that no NPC has. */
    fun unknownNpc(id: String): String = "Unbekannter NPC: $id"

    /** Names a station ID that no station has. */
    fun unknownStation(id: String): String = "Unbekannte Station: $id"

    /** Says that [player] is running another operation. */
    fun busy(player: String): String = "$player führt gerade einen anderen Vorgang aus."

    /** Says that [player] lacks items. */
    fun missingItems(player: String): String = "$player fehlen Gegenstände."

    /** Says that the progression service is unreachable. */
    fun serviceUnavailable(): String = "Der Fortschrittsdienst ist gerade nicht verfügbar."

    /** Says that the service refused an operation for [reason]. */
    fun rejected(reason: String): String = "Abgelehnt: $reason"

    /** Says that the state of [player] is not loaded. */
    fun stateNotLoaded(player: String): String = "Der Zustand von $player ist nicht geladen."

    /** Says that [player] has not accepted [quest]. */
    fun questNotAccepted(player: String, quest: String): String = "$player hat $quest nicht angenommen."

    /** Says that [quest] is not active for [player]. */
    fun questNotActive(player: String, quest: String): String = "$quest ist bei $player nicht aktiv."

    /** Says that [tool] has at most level [max]. */
    fun maxLevel(tool: String, max: Int): String = "$tool hat höchstens Level $max."

    /** Says that the main hand holds no tool. */
    fun noToolInHand(): String = "Du hältst kein Werkzeug in der Hand."

    /** Says that the held tool [toolId] is not part of the content. */
    fun toolNotKnown(toolId: String): String = "Dieses Werkzeug ist nicht bekannt: $toolId"

    /** Says that [tool] is now on [level]. */
    fun levelSet(tool: String, level: Int): String = "$tool ist jetzt Level $level."

    /** Says that no tool is registered with the given ID. */
    fun toolNotRegistered(): String = "Kein Werkzeug mit dieser ID registriert."

    /** Says that the admin looks at no block. */
    fun noBlockTargeted(): String = "Du schaust keinen Block an."

    /** Says that the placement file was written to [path]. */
    fun saved(path: Path): String = "Gespeichert in $path."

    /** Says that [path] exists and is not overwritten. */
    fun fileExists(path: Path): String = "$path existiert bereits und wird nicht überschrieben."

    /** Says that writing [path] failed for [reason]. */
    fun saveFailed(path: Path, reason: String?): String = "Speichern in $path fehlgeschlagen: $reason"

    /** Says that [quest] was given to [player]. */
    fun questGiven(quest: String, player: String): String = "$quest wurde an $player vergeben."

    /** Says that [quest] was completed for [player]. */
    fun questCompleted(quest: String, player: String): String = "$quest wurde für $player abgeschlossen."

    /** Says that [quest] was reset for [player]. */
    fun questReset(quest: String, player: String): String = "$quest wurde für $player zurückgesetzt."

    /** Says that [recipe] was unlocked for [player]. */
    fun recipeGiven(recipe: String, player: String): String = "$recipe wurde für $player freigeschaltet."

    /** Says that [recipe] was taken from [player]. */
    fun recipeTaken(recipe: String, player: String): String = "$recipe wurde $player entzogen."

    /** Says that [title] was given to [player]. */
    fun titleGiven(title: String, player: String): String = "$title wurde an $player vergeben."

    /** Says that [title] was taken from [player]. */
    fun titleTaken(title: String, player: String): String = "$title wurde $player entzogen."

    /** Says that [amount] of [item] were given to [player]. */
    fun itemGiven(item: String, amount: Int, player: String): String = "${amount}x $item wurde an $player gegeben."

    /** Says that [tool] on [level] was given to [player]. */
    fun toolGiven(tool: String, level: Int, player: String): String = "$tool (Level $level) wurde an $player gegeben."
}
