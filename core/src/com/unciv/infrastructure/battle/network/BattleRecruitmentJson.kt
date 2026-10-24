package com.unciv.infrastructure.battle.network

import com.badlogic.gdx.utils.JsonValue
import com.unciv.infrastructure.battle.network.BattleReplyJson.int
import com.unciv.infrastructure.battle.network.BattleReplyJson.required
import com.unciv.infrastructure.battle.network.BattleReplyJson.text
import com.unciv.pure.application.battle.recruitment.*

internal object BattleRecruitmentJson {
    fun decode(value: JsonValue): Pair<BattleArmySelection, List<BattleArmyStack>> {
        require(value.isObject)
        val catalog = value.required("catalog")
        require(catalog.isObject)
        val units = catalog.required("units").also { require(it.isArray) }.map {
            require(it.isObject)
            BattleRecruitmentUnit(it.required("unitName").text(), it.int("cost"))
        }
        val selection = BattleArmySelection(units, catalog.int("budget"), catalog.int("maxStacks"))
        val own = value.required("ownArmy").also { require(it.isArray) }.map {
            require(it.isObject)
            BattleArmyStack(it.required("unitName").text(), it.int("count"))
        }
        return selection to if (own.isEmpty()) emptyList() else selection.validate(own)
    }
}
