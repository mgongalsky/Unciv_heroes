package com.unciv.models.metadata

import com.unciv.Constants
import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.logic.civilization.PlayerType

/** Параметры участника запуска: выбранная нация, управление, сетевой аккаунт и назначение в сценарии. */
class Player(var chosenCiv: String = Constants.random) : IsPartOfGameInfoSerialization {
    var playerType: PlayerType = PlayerType.AI
    var playerId = ""
    /** Стабильный ID слота карты, независимый от нации и аккаунта; пустая строка означает отсутствие назначения. */
    var scenarioSlotId = ""
}
