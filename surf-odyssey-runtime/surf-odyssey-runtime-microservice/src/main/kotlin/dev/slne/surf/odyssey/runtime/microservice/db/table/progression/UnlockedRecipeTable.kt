package dev.slne.surf.odyssey.runtime.microservice.db.table.progression

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableTable
import dev.slne.surf.odyssey.runtime.microservice.db.table.OdysseyPlayerTable

/** The recipes each player has unlocked, one row per player and recipe. */
object UnlockedRecipeTable : AuditableTable("odyssey_unlocked_recipes") {
    /** The player who unlocked the recipe. */
    val player = nativeUuid("player_uuid")
        .references(OdysseyPlayerTable.uuid, onDelete = ReferenceOption.CASCADE)

    /** The id of the recipe definition. */
    val recipeId = varchar("recipe_id", 64)

    override val primaryKey = PrimaryKey(player, recipeId)
}
