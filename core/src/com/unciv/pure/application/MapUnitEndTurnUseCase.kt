package com.unciv.pure.application

import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.pure.domain.supply.RealSupplyMechanic
import com.unciv.pure.domain.supply.SupplyMechanic

object MapUnitEndTurnUseCase {
    fun execute(
        unit: MapUnit,
        civInfo: CivilizationInfo,
        currentTile: TileInfo,
        ruleset: Ruleset,
        supplyMechanic: SupplyMechanic = RealSupplyMechanic(),
        clearPathfindingCache: () -> Unit,
        heal: () -> Unit,
        doCitadelDamage: () -> Unit,
        doTerrainDamage: () -> Unit,
        addMovementMemory: () -> Unit,
        onReligiousStrengthLost: () -> Unit,
        isPreparingParadropOrAirSweep: () -> Boolean = { false }
    ) {
        // Food and army logic — hero mechanic
        if (!unit.isMonster) {
            supplyMechanic.consumeFood(unit.hero, unit.army)
            if (supplyMechanic.isStarving(unit.hero)) {
                supplyMechanic.onStarving(unit.hero, unit.army)
            }
        }

        clearPathfindingCache()

        // Work on improvement — delegate to UseCase directly
        if (unit.currentMovement > 0
                && currentTile.improvementInProgress != null
                && unit.canBuildImprovement(currentTile.getTileImprovementInProgress()!!)
        ) {
            WorkOnImprovementUseCase.execute(
                tile = currentTile,
                civInfo = civInfo,
                ruleset = ruleset,
                mapUnit = unit,
                onImprovementCompleted = { unit.onImprovementCompleted() },
                tryProvideProductionToClosestCity = { }
            )
        }

        // Fortify logic
        if (unit.currentMovement == unit.getMaxMovement().toFloat()
                && unit.isFortified()
                && unit.turnsFortified < 2
        ) unit.turnsFortified++
        if (!unit.isFortified()) unit.turnsFortified = 0

        // Heal
        if (unit.currentMovement == unit.getMaxMovement().toFloat()
                || unit.hasUnique(UniqueType.HealsEvenAfterAction)
        ) heal()

        // Action logic
        if (unit.action != null && unit.health > 99)
            if (unit.isActionUntilHealed()) unit.action = null

        if (isPreparingParadropOrAirSweep())
            unit.action = null

        // Religious strength loss — civ mechanic, wrapped
        onReligiousStrengthLost()

        doCitadelDamage()
        doTerrainDamage()
        addMovementMemory()
    }
}
