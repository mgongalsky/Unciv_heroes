package com.unciv.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.HexMath
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.map.TileMap

object TroopEntersBattleUseCase {
    data class Input(
        val troop: TroopInfo,
        val isPlayerControlled: Boolean,
        val number: Int,
        val isAttacker: Boolean,
        val battleField: TileMap
    )

    fun execute(input: Input) {
        input.troop.isPlayerControlledOverride = input.isPlayerControlled

        val positionToSet = if (input.isAttacker)
            HexMath.evenQ2HexCoords(Vector2(-7f, 3f - input.number.toFloat() * 2))
        else
            HexMath.evenQ2HexCoords(Vector2(6f, 3f - input.number.toFloat() * 2))

        input.troop.currentTile = input.battleField[positionToSet]
        input.troop.currentMovement = input.troop.speed.toFloat()
        //input.troop.currentTile.troopUnit = input.troop
        input.troop.currentHealth = input.troop.maxHealth
        input.troop.currentAmount = input.troop.amount
        input.troop.battleField = input.battleField
    }
}
