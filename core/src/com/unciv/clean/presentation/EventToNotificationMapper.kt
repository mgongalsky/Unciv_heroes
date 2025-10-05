package com.unciv.clean.presentation

import com.unciv.clean.domain.events.DomainEvent
import com.unciv.clean.domain.events.Scope
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.LocationAction
import com.unciv.logic.civilization.NotificationIcon

object EventToNotificationMapper {
    fun apply(events: List<DomainEvent>, getCiv: (String) -> CivilizationInfo) {
        for (e in events) when (e) {
            is DomainEvent.TurnAdvanced -> {
                // Пока ничего не показываем, точка расширения
            }
            is DomainEvent.EnemyUnitsSpotted -> {
                val civ = getCiv(e.civName)
                val scopeText = if (e.scope == Scope.IN_TERRITORY) "in" else "near"
                if (e.positions.size < 3) {
                    e.positions.forEach { pos ->
                        civ.addNotification(
                            "Enemy unit spotted $scopeText our territory",
                            pos,
                            NotificationIcon.War
                        )
                    }
                } else {
                    civ.addNotification(
                        "[${e.positions.size}] enemy units were spotted $scopeText our territory",
                        LocationAction(e.positions.asSequence()),
                        NotificationIcon.War
                    )
                }
            }
            is DomainEvent.CitiesCanBombard -> {
                val civ = getCiv(e.civName)
                if (e.positions.size < 3) {
                    e.positions.forEach { loc ->
                        civ.addNotification(
                            "Your city can bombard the enemy!",
                            loc,
                            NotificationIcon.City,
                            NotificationIcon.Crosshair
                        )
                    }
                } else {
                    civ.addNotification(
                        "[${e.positions.size}] of your cities can bombard the enemy!",
                        LocationAction(e.positions.asSequence()),
                        NotificationIcon.City,
                        NotificationIcon.Crosshair
                    )
                }
            }
            is DomainEvent.ResourcesRevealed -> {
                val civ = getCiv(e.civName)
                val title = if (e.positions.size == 1)
                    "[${e.resourceName}] revealed near [${e.nearCity ?: "a city"}]"
                else
                    "[${e.positions.size}] sources of [${e.resourceName}] revealed, e.g. near [${e.nearCity ?: "a city"}]"
                civ.addNotification(
                    title,
                    LocationAction(e.positions.asSequence()),
                    "ResourceIcons/${e.resourceName}"
                )
            }
        }
    }
}
