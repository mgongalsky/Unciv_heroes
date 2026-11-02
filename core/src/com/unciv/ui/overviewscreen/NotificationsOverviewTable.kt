package com.unciv.ui.overviewscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.UncivGame
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.Notification
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.WrappableLabel
import com.unciv.ui.utils.TabbedPager
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.worldscreen.WorldScreen

class NotificationsOverviewTable(
    val worldScreen: WorldScreen,
    viewingPlayer: CivilizationInfo,
    overviewScreen: EmpireOverviewScreen,
    persistedData: EmpireOverviewTabPersistableData? = null
) : EmpireOverviewTab(viewingPlayer, overviewScreen) {
    class NotificationsTabPersistableData(
            var scrollY: Float? = null
    ) : EmpireOverviewTabPersistableData() {
        override fun isEmpty() = scrollY == null
    }
    override val persistableData = (persistedData as? NotificationsTabPersistableData) ?: NotificationsTabPersistableData()
    override fun activated(index: Int, caption: String, pager: TabbedPager) {
        if (persistableData.scrollY != null)
            pager.setPageScrollY(index, persistableData.scrollY!!)
        super.activated(index, caption, pager)
    }
    override fun deactivated(index: Int, caption: String, pager: TabbedPager) {
        persistableData.scrollY = pager.getPageScrollY(index)
    }

    val notificationLog = viewingPlayer.notificationsLog

    private val notificationTable = Table(BaseScreen.skin)

    val scaleFactor = 0.3f
    val inverseScaleFactor = 1f / scaleFactor
    private val maxWidthOfStage = 0.333f
    private val maxEntryWidth = worldScreen.stage.width * maxWidthOfStage * inverseScaleFactor

    val iconSize = 20f

    init {
        val tablePadding = 30f
        defaults().pad(tablePadding).top()

        generateNotificationTable()

        add(notificationTable)
    }

    private fun generateNotificationTable() {
        if (viewingPlayer.notifications.isNotEmpty())
            notificationTable.add(notificationsArrayTable("Current", viewingPlayer.notifications)).row()

        for (notification in notificationLog.asReversed()) {
            notificationTable.add(notificationsArrayTable(notification.turn.toString(), notification.notifications))
            notificationTable.padTop(20f).row()
        }
    }

    private fun notificationsArrayTable(
        index: String,
        notifications: ArrayList<Notification>
    ): Table {
        val royal = overviewScreen.game.settings.skin == "Minimal"
        val turnTable = Table(BaseScreen.skin)
        val title = if (index != "Current") "Turn [$index]" else "Current turn"
        val contentWidth = minOf(720f, overviewScreen.stage.width - 100f).coerceAtLeast(48f)
        if (royal) {
            turnTable.background = BaseScreen.skinStrings.getUiBackground(
                "OverviewScreen/Notifications/Turn", tintColor = Color.valueOf("EADFBD")
            )
            turnTable.pad(10f)
        }
        val turnLabel =
            title.toLabel(fontColor = if (royal) Color.valueOf("453925") else Color.WHITE)
        val titleCell = turnTable.add(turnLabel)
        if (royal) {
            turnLabel.setWrap(true)
            titleCell.width(contentWidth)
        }
        titleCell.row()

        for (notification in notifications) {
            val entry = Table(BaseScreen.skin)
            entry.name = "overviewNotificationEntry"
            if (royal) {
                entry.background = BaseScreen.skinStrings.getUiBackground(
                    "OverviewScreen/NotificationOverviewTable/Notification",
                    BaseScreen.skinStrings.roundedEdgeRectangleShape,
                    tintColor = Color.valueOf("EADFBD")
                )
                entry.pad(10f)
                val label =
                    WrappableLabel(notification.text, contentWidth, Color.valueOf("453925"), 20)
                label.setWrap(true)
                entry.add(label).width(contentWidth).left().row()
                if (notification.icons.isNotEmpty()) {
                    val icons = Table()
                    notification.addNotificationIcons(worldScreen.gameInfo.ruleSet, iconSize, icons)
                    val iconScroll = com.unciv.ui.utils.AutoScrollPane(icons).apply {
                        setScrollingDisabled(false, true)
                        setOverscroll(false, false)
                    }
                    entry.add(iconScroll).width(contentWidth).padTop(5f).row()
                }
            } else {
                val labelWidth = maxEntryWidth * notification.icons.size - 10f
                entry.add(WrappableLabel(notification.text, labelWidth, Color.BLACK, 20))
                entry.background = BaseScreen.skinStrings.getUiBackground(
                    "OverviewScreen/NotificationOverviewTable/Notification",
                    BaseScreen.skinStrings.roundedEdgeRectangleShape
                )
                notification.addNotificationIcons(worldScreen.gameInfo.ruleSet, iconSize, entry)
            }
            entry.touchable = Touchable.enabled
            entry.onClick {
                UncivGame.Current.resetToWorldScreen()
                notification.action?.execute(worldScreen)
            }
            turnTable.add(entry).padTop(5f)
            turnTable.padTop(20f).row()
        }
        turnTable.padTop(20f).row()
        return turnTable
    }
}
