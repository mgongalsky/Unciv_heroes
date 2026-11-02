package com.unciv.ui.trade

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.toLabel

/** Представление лидера; parchmentWidth включает светлую адаптивную компоновку явно. */
class LeaderIntroTable(
    civInfo: CivilizationInfo,
    hello: String = "",
    parchmentWidth: Float = 0f
) : Table(BaseScreen.skin) {
    init {
        defaults().align(Align.center)
        val nation = civInfo.nation
        val leaderPortraitFile = "LeaderIcons/" + nation.leaderName
        val hasPortrait =
            nation.leaderName.isNotEmpty() && ImageGetter.imageExists(leaderPortraitFile)
        val royal = parchmentWidth > 0f
        val ink = if (royal) com.badlogic.gdx.graphics.Color.valueOf("453925")
        else com.badlogic.gdx.graphics.Color.WHITE
        val leaderLabel = civInfo.getLeaderDisplayName().toLabel(
            fontColor = ink, fontSize = Constants.headingFontSize
        )
        val nationIndicator = ImageGetter.getNationIndicator(nation, 24f)
        if (royal) {
            leaderLabel.wrap = true
            leaderLabel.setAlignment(Align.center)
            val nameTable = Table()
            val textWidth = if (hasPortrait && parchmentWidth >= 400f)
                parchmentWidth - 110f else parchmentWidth
            nameTable.add(nationIndicator).padBottom(8f).row()
            nameTable.add(leaderLabel).width(textWidth).row()
            if (hello.isNotEmpty()) {
                nameTable.add(hello.toLabel(fontColor = ink).apply {
                    wrap = true
                    setAlignment(Align.center)
                }).width(textWidth).padTop(8f).row()
            }
            if (hasPortrait) {
                val portrait = ImageGetter.getImage(leaderPortraitFile).apply {
                    setScaling(com.badlogic.gdx.utils.Scaling.fit)
                }
                val portraitCell = add(portrait).size(minOf(100f, parchmentWidth))
                if (parchmentWidth < 400f) portraitCell.padBottom(10f).row()
                else portraitCell.padRight(10f)
            }
            add(nameTable).width(textWidth)
        } else if (hasPortrait) {
            val nameTable = Table()
            nameTable.add(leaderLabel)
            nameTable.add(nationIndicator).pad(0f, 10f, 5f, 0f).row()
            if (hello.isNotEmpty()) nameTable.add(hello.toLabel()).colspan(2)
            add(ImageGetter.getImage(leaderPortraitFile)).size(100f).padRight(10f)
            add(nameTable)
        } else {
            add(nationIndicator).pad(0f, 0f, 5f, 10f)
            add(leaderLabel).row()
            if (hello.isNotEmpty()) add(hello.toLabel()).colspan(2)
        }
    }
}
