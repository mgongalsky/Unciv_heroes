package com.unciv.logic

import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo
import com.unciv.logic.map.UnitMovementAlgorithms
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.ruleset.unit.BaseUnit

open class MovableUnit {

    @Transient
    open lateinit var civInfo: CivilizationInfo

    fun isCivilizationInfoInitialized(): Boolean = ::civInfo.isInitialized

    @Transient
    open lateinit var currentTile: TileInfo

    fun isCurrentTileInitialized(): Boolean = ::currentTile.isInitialized

    var currentMovement: Float = 0f




    @Transient
    lateinit var baseUnit: BaseUnit

    fun isBaseUnitInitialized(): Boolean = ::baseUnit.isInitialized


    @Transient
    val doubleMovementInTerrain = HashMap<String, MapUnit.DoubleMovementTerrainTarget>()

    @Transient
    var canEnterIceTiles = false

    @Transient
    var costToDisembark: Float? = null


    @Transient
    var costToEmbark: Float? = null


    @Transient
    var paradropRange = 0



    @Transient
    var ignoresTerrainCost = false
        protected set

    @Transient
    var ignoresZoneOfControl = false
        protected set

    /** If set causes an early exit in getMovementCostBetweenAdjacentTiles
     *  - means no double movement uniques, roughTerrainPenalty or ignoreHillMovementCost */
    @Transient
    var noTerrainMovementUniques = false
        protected set

    @Transient
    var roughTerrainPenalty = false
        protected set

    /** If set causes a second early exit in getMovementCostBetweenAdjacentTiles */
    @Transient
    var noBaseTerrainOrHillDoubleMovementUniques = false
        protected set

    /** If set skips tile.matchesFilter tests for double movement in getMovementCostBetweenAdjacentTiles */
    @Transient
    var noFilteredDoubleMovementUniques = false
        protected set

    @Transient
    val movement = UnitMovementAlgorithms(this)

    @Transient
    var canPassThroughImpassableTiles = false
        protected set


    @Transient
    var canEnterForeignTerrain: Boolean = false



    fun getTile(): TileInfo = currentTile

    open fun getMaxMovement(): Int {
        var movement = baseUnit.movement

        return movement
    }


    open fun getDamageFromTerrain(tile: TileInfo = currentTile): Int {
        //if (civInfo.nonStandardTerrainDamage) {
       // }
        // Otherwise fall back to the defined standard damage
        return 0
    }

    open fun removeFromTile() {
        TODO("Not yet implemented")
    }

    open fun moveThroughTile(tile: TileInfo) {

    }

    open fun useMovementPoints(amount: Float) {
        currentMovement -= amount
        if (currentMovement < 0) currentMovement = 0f


    }

    open fun putInTile(tile: TileInfo) {
        currentTile = tile

    }

    open fun getRange(): Int {
        if (baseUnit.isMelee()) return 1
        return baseUnit.range
    }
}
