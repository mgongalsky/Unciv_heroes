package com.unciv.logic.automation.unit

import com.unciv.Constants
import com.unciv.logic.battle.Battle
import com.unciv.logic.battle.BattleDamage
import com.unciv.logic.battle.ICombatant
import com.unciv.logic.battle.MapUnitCombatant
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.PathsToTilesWithinTurn
import com.unciv.logic.map.TileInfo
import com.unciv.models.AttackableTile
import com.unciv.models.ruleset.unique.UniqueType

object BattleHelper {

    fun tryAttackNearbyEnemy(unit: MapUnit, stayOnTile: Boolean = false): Boolean {
        if (unit.hasUnique(UniqueType.CannotAttack)) return false
        val attackableEnemies = getAttackableEnemies(unit, unit.movement.getDistanceToTiles(), stayOnTile = stayOnTile)
            // Only take enemies we can fight without dying
            .filter {
                val combatant = Battle.getMapCombatantOfTile(it.tileToAttack)
                combatant != null && // Exclude null combatants
                        BattleDamage.calculateDamageToAttacker(
                            MapUnitCombatant(unit),
                            combatant
                        ) + unit.getDamageFromTerrain(it.tileToAttackFrom) < unit.health
            }

        val enemyTileToAttack = chooseAttackTarget(unit, attackableEnemies)

        if (enemyTileToAttack != null) {
            Battle.moveAndAttack(MapUnitCombatant(unit), enemyTileToAttack)
        }
        return unit.currentMovement == 0f
    }

    /**
     * Identifies all tiles that a unit can attack based on its range, movement, and other attributes.
     * This function calculates potential attackable tiles and considers unit-specific conditions,
     * such as setup requirements or protection by enemy units.
     *
     * @param unit The unit attempting to attack.
     * @param unitDistanceToTiles A map of tiles reachable within the unit's current movement points.
     * @param tilesToCheck Optional list of tiles to limit the scope of the check.
     * @param stayOnTile If true, the unit does not move and attacks from its current position.
     * @return A list of `AttackableTile` objects representing tiles the unit can attack.
     */
    fun getAttackableEnemies(
        unit: MapUnit,
        unitDistanceToTiles: PathsToTilesWithinTurn,
        tilesToCheck: List<TileInfo>? = null,
        stayOnTile: Boolean = false
    ): ArrayList<AttackableTile> {
        // TODO: This logic may need adjustment when scouting mechanics are implemented.
        val scoutingRange = 1
        val rangeOfAttack = unit.getRange() + scoutingRange
        val attackableTiles = ArrayList<AttackableTile>()

        // Determine if the unit must be set up before attacking.
        val unitMustBeSetUp = unit.hasUnique(UniqueType.MustSetUp)
        val tilesToAttackFrom = if (stayOnTile || unit.baseUnit.movesLikeAirUnits())
            sequenceOf(Pair(unit.currentTile, unit.currentMovement)) // Attack from the current position if staying in place or if it's an air unit.
        else
            unitDistanceToTiles.asSequence()
                .map { (tile, distance) ->
                    // Calculate remaining movement points after moving to the tile.
                    val movementPointsToExpendAfterMovement = if (unitMustBeSetUp) 1 else 0
                    val movementPointsToExpendHere =
                            if (unitMustBeSetUp && !unit.isSetUpForSiege()) 1 else 0
                    val movementPointsToExpendBeforeAttack =
                            if (tile == unit.currentTile) movementPointsToExpendHere else movementPointsToExpendAfterMovement
                    val movementLeft =
                            unit.currentMovement - distance.totalDistance - movementPointsToExpendBeforeAttack
                    Pair(tile, movementLeft)
                }
                // Filter tiles where the unit has enough movement points left to attack.
                .filter { it.second > Constants.minimumMovementEpsilon }
                .filter {
                    // Check if the unit can move to the tile or if it is protected by an enemy unit.
                    it.first == unit.getTile() || unit.movement.canMoveTo(it.first) || it.first.hasEnemyProtector(unit.civInfo)
                }

        val tilesWithEnemies: HashSet<TileInfo> = HashSet()
        val tilesWithoutEnemies: HashSet<TileInfo> = HashSet()
        for ((reachableTile, movementLeft) in tilesToAttackFrom) { // Iterate through reachable tiles with enough movement points.
            val tilesInAttackRange =
                    reachableTile.getTilesInDistance(rangeOfAttack) // Calculate tiles within attack range.
                        .asSequence()

            for (tile in tilesInAttackRange) {
                if (tile in tilesWithEnemies) {
                    // Add previously identified attackable tiles to the list.
                    attackableTiles += AttackableTile(reachableTile, tile, movementLeft)
                } else if (tile in tilesWithoutEnemies) {
                    // Skip tiles that have already been confirmed as empty.
                    continue
                } else if (checkTile(unit, tile, tilesToCheck) || tile.hasEnemyProtector(unit.civInfo)) {
                    // Check if the tile is valid for attacking or is protected by an enemy unit.
                    tilesWithEnemies += tile
                    attackableTiles += AttackableTile(reachableTile, tile, movementLeft)
                } else if (unit.isPreparingAirSweep()) {
                    // Add tiles if the unit is preparing an air sweep.
                    tilesWithEnemies += tile
                    attackableTiles += AttackableTile(reachableTile, tile, movementLeft)
                } else {
                    // Mark tile as empty.
                    tilesWithoutEnemies += tile
                }
            }
        }
        return attackableTiles
    }

    private fun checkTile(unit: MapUnit, tile: TileInfo, tilesToCheck: List<TileInfo>?): Boolean {
        if (!containsAttackableEnemy(tile, MapUnitCombatant(unit))) return false
        if (tile !in (tilesToCheck ?: unit.civInfo.viewableTiles)) return false
        val mapCombatant = Battle.getMapCombatantOfTile(tile)
        return (!unit.baseUnit.isMelee() || mapCombatant !is MapUnitCombatant || !mapCombatant.unit.isCivilian() || unit.movement.canPassThrough(tile))
    }

    fun containsAttackableEnemy(tile: TileInfo, combatant: ICombatant): Boolean {
        if (combatant is MapUnitCombatant && combatant.unit.isEmbarked() && !combatant.hasUnique(UniqueType.AttackOnSea)) {
            // Can't attack water units while embarked, only land
            if (tile.isWater || combatant.isRanged())
                return false
        }

        val tileCombatant = Battle.getMapCombatantOfTile(tile) ?: return false
        if (tileCombatant.getCivInfo() == combatant.getCivInfo()) return false
        if (!combatant.getCivInfo().isAtWarWith(tileCombatant.getCivInfo())) return false

        if (combatant is MapUnitCombatant && combatant.isLandUnit() && combatant.isMelee() &&
            !combatant.hasUnique(UniqueType.LandUnitEmbarkation) && tile.isWater
        )
            return false

        if (combatant is MapUnitCombatant &&
            combatant.unit.hasUnique(UniqueType.CanOnlyAttackUnits) &&
            combatant.unit.getMatchingUniques(UniqueType.CanOnlyAttackUnits).none { tileCombatant.matchesCategory(it.params[0]) }
        )
            return false

        if (combatant is MapUnitCombatant &&
            combatant.unit.getMatchingUniques(UniqueType.CanOnlyAttackTiles)
                .let { unique -> unique.any() && unique.none { tile.matchesFilter(it.params[0]) } }
        )
            return false

        // Only units with the right unique can view submarines (or other invisible units) from more then one tile away.
        // Garrisoned invisible units can be attacked by anyone, as else the city will be in invincible.
        if (tileCombatant.isInvisible(combatant.getCivInfo()) && !tile.isCityCenter()) {
            return combatant is MapUnitCombatant
                && combatant.getCivInfo().viewableInvisibleUnitsTiles.map { it.position }.contains(tile.position)
        }
        return true
    }

    fun tryDisembarkUnitToAttackPosition(unit: MapUnit): Boolean {
        if (!unit.baseUnit.isMelee() || !unit.baseUnit.isLandUnit() || !unit.isEmbarked()) return false
        val unitDistanceToTiles = unit.movement.getDistanceToTiles()

        val attackableEnemiesNextTurn = getAttackableEnemies(unit, unitDistanceToTiles)
                // Only take enemies we can fight without dying
                .filter {
                    BattleDamage.calculateDamageToAttacker(
                        MapUnitCombatant(unit),
                        Battle.getMapCombatantOfTile(it.tileToAttack)!!
                    ) < unit.health
                }
                .filter { it.tileToAttackFrom.isLand }

        val enemyTileToAttackNextTurn = chooseAttackTarget(unit, attackableEnemiesNextTurn)

        if (enemyTileToAttackNextTurn != null) {
            unit.movement.moveToTile(enemyTileToAttackNextTurn.tileToAttackFrom)
            return true
        }
        return false
    }

    private fun chooseAttackTarget(unit: MapUnit, attackableEnemies: List<AttackableTile>): AttackableTile? {
        val cityTilesToAttack = attackableEnemies.filter { it.tileToAttack.isCityCenter() }
        val nonCityTilesToAttack = attackableEnemies.filter { !it.tileToAttack.isCityCenter() }

        // todo For air units, prefer to attack tiles with lower intercept chance

        val capturableCity = cityTilesToAttack.firstOrNull { it.tileToAttack.getCity()!!.health == 1 }
        val cityWithHealthLeft =
            cityTilesToAttack.filter { it.tileToAttack.getCity()!!.health != 1 } // don't want ranged units to attack defeated cities
                .minByOrNull { it.tileToAttack.getCity()!!.health }

        if (unit.baseUnit.isMelee() && capturableCity != null)
            return capturableCity // enter it quickly, top priority!

        else if (nonCityTilesToAttack.isNotEmpty()) // second priority, units
            return chooseUnitToAttack(unit, nonCityTilesToAttack)

        else if (cityWithHealthLeft != null) return cityWithHealthLeft // third priority, city

        return null
    }

    private fun chooseUnitToAttack(unit: MapUnit, attackableUnits: List<AttackableTile>): AttackableTile {
        val militaryUnits = attackableUnits.filter { it.tileToAttack.militaryUnit != null }

        // prioritize attacking military
        if (militaryUnits.isNotEmpty()) {
            // associate enemy units with number of hits from this unit to kill them
            val attacksToKill = militaryUnits
                .associateWith { it.tileToAttack.militaryUnit!!.health.toFloat() / BattleDamage.calculateDamageToDefender(
                        MapUnitCombatant(unit),
                        MapUnitCombatant(it.tileToAttack.militaryUnit!!)
                    ).toFloat().coerceAtLeast(1f) }

            // kill a unit if possible, prioritizing by attack strength
            val canKill = attacksToKill.filter { it.value <= 1 }
                .maxByOrNull { MapUnitCombatant(it.key.tileToAttack.militaryUnit!!).getAttackingStrength() }?.key
            if (canKill != null) return canKill

            // otherwise pick the unit we can kill the fastest
            return attacksToKill.minByOrNull { it.value }!!.key
        }

        // only civilians in attacking range - GP most important, second settlers, then anything else

        val unitsToConsider = attackableUnits.filter { it.tileToAttack.civilianUnit!!.isGreatPerson() }
            .ifEmpty { attackableUnits.filter { it.tileToAttack.civilianUnit!!.hasUnique(UniqueType.FoundCity) } }
            .ifEmpty { attackableUnits }

        // Melee - prioritize by distance, so we have most movement left
        if (unit.baseUnit.isMelee()){
            return unitsToConsider.maxByOrNull { it.movementLeftAfterMovingToAttackTile }!!
        }

        // We're ranged, prioritize what we can kill
        return unitsToConsider.minByOrNull {
            Battle.getMapCombatantOfTile(it.tileToAttack)!!.getHealth()
        }!!
    }
}
