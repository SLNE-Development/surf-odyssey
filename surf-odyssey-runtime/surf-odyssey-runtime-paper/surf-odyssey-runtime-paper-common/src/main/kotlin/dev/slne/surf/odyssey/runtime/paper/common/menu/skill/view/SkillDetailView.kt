@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.common.menu.skill.view

import dev.slne.surf.api.paper.inventory.framework.dsl.openForPlayer
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.modifyConfig
import dev.slne.surf.api.paper.inventory.framework.titleBuilder
import dev.slne.surf.api.paper.inventory.framework.view.layoutTarget
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.onOpen
import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationEmptyRows
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.api.paper.inventory.framework.view.state.get
import dev.slne.surf.api.paper.inventory.framework.view.state.initialState
import dev.slne.surf.odyssey.core.common.skill.SkillCurve
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.common.menu.skill.createLevelItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.skill.createSkillSummaryItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.backItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.common.skill.displayName

val skillDetailView: AbstractPaginatedSurfView = paginatedSurfView("Skill") {
    val skillState = initialState<SkillType>()

    // One empty row under the header for the skill item, two rows of levels and the row with the page arrows
    settings {
        paginationViewRows(PaginationViewRows.THREE)
        paginationEmptyRows(PaginationEmptyRows.ONE)
    }

    layoutTarget('L')

    pagination<Int> {
        source { (1..SkillCurve.MAX_LEVEL).toList() }
        elementFactory { context, builder, _, level ->
            builder.withItem(createLevelItem(context.player, skillState[context], level))
        }
    }

    onOpen {
        val skill = skillState[this]

        modifyConfig {
            titleBuilder {
                primary(skill.displayName)
            }
        }
    }

    onFirstRender {
        val viewer = player
        val skill = skillState[this]

        slot(1, 5) {
            withItem(createSkillSummaryItem(viewer, skill))
        }

        slot(4, 1, backItem).onClick { click ->
            playGeneralClickSound()

            click.openForPlayer(skillView)
        }
    }
}
