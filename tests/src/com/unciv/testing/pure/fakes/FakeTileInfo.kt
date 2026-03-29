package com.unciv.testing.pure.fakes

import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.RoadStatus
import com.unciv.logic.map.TileInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class FakeTileInfo : TileInfo(), KoinComponent {

    // Inject ruleset from Koin so we don't need a real TileMap or GameInfo
    init {
        ruleset = get()
    }

    // --- Configurable overrides ---
    var isCityCenterOverride: Boolean = false

    // --- Tracked calls ---
    var removeRoadCalled = false
    var addRoadCalled = false
    var setRepairedCalled = false
    var removeImprovementCalled = false

    // --- Override configurable methods ---
    override fun isCityCenter(): Boolean = isCityCenterOverride

    // --- Override problematic methods that touch tileMap/owningCity ---

    override fun removeRoad() {
        removeRoadCalled = true
    }

    override fun addRoad(roadType: RoadStatus, unitCivInfo: CivilizationInfo) {
        addRoadCalled = true
        roadStatus = roadType
    }

    override fun setRepaired() {
        setRepairedCalled = true
    }

    override fun removeImprovement() {
        removeImprovementCalled = true
        improvement = null
    }
}
