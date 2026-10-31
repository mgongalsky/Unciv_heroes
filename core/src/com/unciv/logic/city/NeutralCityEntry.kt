package com.unciv.logic.city

import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.city.NeutralCityEntryRules
import com.unciv.pure.domain.city.NeutralCityEntryRules.Decision

/** Общие правила входа для игрока и AI: связывают чистое решение с реальными юнитами и перемещением. */
object NeutralCityEntry {
    /** Только определяет действие; осмотр цели не должен менять владельца или начинать бой. */
    fun decision(city: CityInfo, unit: MapUnit): Decision {
        // Отдельного надёжного признака героя пока нет: подходит живой наземный военный юнит игрока/AI.
        val eligible = !unit.isDestroyed && unit.isMilitary() && unit.baseUnit.isLandUnit() &&
            !unit.civInfo.isBarbarian() && !unit.civInfo.isSpectator() &&
            unit.civInfo.gameInfo === city.gameInfo
        val defenders = city.state.garrison?.getAllTroops()
            ?.count { it != null && it.currentAmount > 0 } ?: 0
        return NeutralCityEntryRules.decide(city.state.ownership, eligible, defenders)
    }

    /** Для кнопки занятия: проверяет, что пустой центр достижим именно в текущем ходу. */
    fun canEnterThisTurn(city: CityInfo, unit: MapUnit): Boolean {
        if (decision(city, unit) != Decision.ACQUIRE || unit.currentMovement <= 0f) return false
        val center = city.getCenterTile()
        return unit.movement.canMoveTo(center) && unit.movement.canReach(center) &&
            unit.movement.getTileToMoveToThisTurn(center) == center
    }

    /** Перепроверяет цель перед действием и тратит обычное движение; атаку не расходует. */
    fun enterThisTurn(city: CityInfo, unit: MapUnit): Boolean {
        if (!canEnterThisTurn(city, unit)) return false
        unit.action = null
        // Владение меняет обработчик фактического входа ниже, а не сама кнопка интерфейса.
        unit.movement.moveToTile(city.getCenterTile())
        return unit.currentTile == city.getCenterTile() && city.ownerOrNull === unit.civInfo
    }

    /** Вызывается при перемещении: приобрести город можно только уже находясь в его пустом центре. */
    fun tryAcquireOnEntry(unit: MapUnit, tile: TileInfo): Boolean {
        if (!tile.isCityCenter()) return false
        val city = tile.getCity() ?: return false
        if (city.state.ownership != com.unciv.pure.domain.city.CityOwnership.Neutral) return false
        if (unit.currentTile !== tile || decision(city, unit) != Decision.ACQUIRE) return false
        NeutralCityOwnership.acquireUndefended(city, unit.civInfo)
        return true
    }
}
