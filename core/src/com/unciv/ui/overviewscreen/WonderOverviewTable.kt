package com.unciv.ui.overviewscreen

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.UncivGame
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.WonderInfo
import com.unciv.ui.civilopedia.CivilopediaScreen
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel

class WonderOverviewTab(
    viewingPlayer: CivilizationInfo,
    overviewScreen: EmpireOverviewScreen
) : EmpireOverviewTab(viewingPlayer, overviewScreen) {
    val ruleSet = gameInfo.ruleSet

    val wonderInfo = WonderInfo()
    private val wonders: Array<WonderInfo.WonderInfo> = wonderInfo.collectInfo()

    private val fixedContent = Table()
    override fun getFixedContent() = fixedContent

    init {
        fixedContent.apply {
            defaults().pad(10f).align(Align.center)
            add()
            add("Name".toLabel())
            add("Status".toLabel())
            add("Location".toLabel())
            add().minWidth(30f)
            row()
        }

        top()
        defaults().pad(10f).align(Align.center)
        (1..5).forEach { _ -> add() }  // dummies so equalizeColumns can work because the first grid cell is colspan(5)
        row()

        createGrid()

        equalizeColumns(fixedContent, this)
    }

    fun createGrid() {
        val royal = overviewScreen.game.settings.skin == "Minimal"
        val ink =
            if (royal) com.badlogic.gdx.graphics.Color.valueOf("453925") else com.badlogic.gdx.graphics.Color.WHITE
        if (royal) {
            background = com.unciv.ui.utils.BaseScreen.skinStrings.getUiBackground(
                "EmpireOverview/Wonders/Panel",
                tintColor = com.badlogic.gdx.graphics.Color.valueOf("EADFBD")
            )
            fixedContent.background = com.unciv.ui.utils.BaseScreen.skinStrings.getUiBackground(
                "EmpireOverview/Wonders/Header",
                tintColor = com.badlogic.gdx.graphics.Color.valueOf("EADFBD")
            )
            for (label in fixedContent.children.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>())
                label.color = ink
        }
        var lastGroup = ""
        for (wonder in wonders) {
            if (wonder.status == WonderInfo.WonderStatus.Hidden) continue
            if (wonder.groupName != lastGroup) {
                lastGroup = wonder.groupName
                val groupRow = Table().apply {
                    add(ImageGetter.getDot(wonder.groupColor)).minHeight(2f).growX()
                    val label = lastGroup.toLabel(if (royal) ink else wonder.groupColor).apply {
                        setAlignment(Align.right)
                        if (royal) setWrap(true)
                    }
                    val cell = add(label).padLeft(1f).right()
                    if (royal) cell.width(220f)
                }
                add(groupRow).fillX().colspan(5).padBottom(0f).row()
            }

            val image = wonder.getImage()
            image?.onClick {
                UncivGame.Current.pushScreen(
                    CivilopediaScreen(
                        ruleSet,
                        wonder.category,
                        wonder.name
                    )
                )
            }
            add(image).pad(0f, 10f, 0f, 10f)

            val nameLabel = wonder.getNameColumn().toLabel(fontColor = ink)
            val nameCell = add(nameLabel).pad(15f, 10f, 15f, 10f)
            val statusLabel = wonder.getStatusColumn().toLabel(fontColor = ink)
            val statusCell = add(statusLabel)
            if (royal) {
                nameLabel.setWrap(true)
                statusLabel.setWrap(true)
                nameCell.width(180f)
                statusCell.width(120f)
            }
            val locationText = wonder.getLocationColumn()
            if (locationText.isNotEmpty()) {
                val locationLabel = locationText.toLabel(fontColor = ink)
                if (wonder.location != null)
                    locationLabel.onClick {
                        val worldScreen = UncivGame.Current.resetToWorldScreen()
                        worldScreen.mapHolder.setCenterPosition(wonder.location.position)
                    }
                val locationCell = add(locationLabel).fillY()
                if (royal) {
                    locationLabel.setWrap(true)
                    locationCell.width(180f)
                }
            }
            row()
        }
    }
}
