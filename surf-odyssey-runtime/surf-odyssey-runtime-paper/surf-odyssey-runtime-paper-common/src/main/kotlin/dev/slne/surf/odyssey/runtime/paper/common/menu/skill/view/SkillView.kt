package dev.slne.surf.odyssey.runtime.paper.common.menu.skill.view

import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.openForPlayer
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.view.*
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.blockRow
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewRows
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.common.menu.skill.createSkillOverviewItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound

val skillView: AbstractSurfView = surfView("Skills") {
    settings {
        rows(ViewRows.THREE)
        navigateBackOnOutsideClick(false)
    }

    containerDefaults {
        blockRow(1)
        blockRow(2, exemptColumns = intArrayOf())
        blockRow(3)
    }

    // Built on every open, so each player sees their own values
    onFirstRender {
        val viewer = player

        SkillType.entries.zip(intArrayOf(3, 5, 7).toList()).forEach { (skill, column) ->
            slot(2, column) {
                withItem(createSkillOverviewItem(viewer, skill))

                onItemClick {
                    playGeneralClickSound()
                    openForPlayer(skillDetailView, skill)
                }
            }
        }
    }
}
