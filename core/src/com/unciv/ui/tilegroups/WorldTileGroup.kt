package com.unciv.ui.tilegroups

import com.unciv.UncivGame
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo
import com.unciv.ui.worldscreen.WorldScreen


class WorldTileGroup(internal val worldScreen: WorldScreen, tileInfo: TileInfo, tileSetStrings: TileSetStrings)
    : TileGroup(tileInfo,tileSetStrings) {

    private var cityButton: CityButton? = null

    fun selectUnit(unit: MapUnit) {
        if(unit.baseUnit.movesLikeAirUnits()) return // doesn't appear on map so nothing to select
        val unitImage = if (unit.isCivilian()) icons.civilianUnitIcon
        else icons.militaryUnitIcon
        unitImage?.selectUnit()
    }

    /** Обновляет клетку и подпись города по видимости; значок работающего населения доступен только владельцу. */
    fun update(viewingCiv: CivilizationInfo) {
        val city = tileInfo.getCity()

        icons.removePopulationIcon()
        val tileIsViewable = isViewable(viewingCiv)
        val showEntireMap = UncivGame.Current.viewEntireMapForDebug

        // Наличие города на клетке не гарантирует наличие цивилизации: у нейтрального владельца нет.
        if (tileIsViewable && tileInfo.isWorked() && UncivGame.Current.settings.showWorkedTiles
            && city?.ownerOrNull == viewingCiv
        )
            icons.addPopulationIcon()
        if (showEntireMap
            || viewingCiv.hasExplored(tileInfo)
            || viewingCiv.isSpectator()
            || (worldScreen.viewingCiv.isSpectator() && !worldScreen.fogOfWar)
        ) {
            // Подпись добавляется до остальных слоёв, чтобы юниты оставались над ней.
            updateCityButton(city, tileIsViewable || showEntireMap)
        } else if (worldScreen.viewingCiv.isSpectator() && worldScreen.fogOfWar) {
            updateCityButton(null, showEntireMap)
        }

        super.update(
            viewingCiv,
            UncivGame.Current.settings.showResourcesAndImprovements,
            UncivGame.Current.settings.showTileYields
        )
    }


    private fun updateCityButton(city: CityInfo?, viewable: Boolean) {
        if (city == null && cityButton != null)// there used to be a city here but it was razed
        {
            cityButton!!.remove()
            cityButton = null
        }
        if (city != null && tileInfo.isCityCenter()) {
            if (cityButton == null) {
                cityButton = CityButton(city, this)
                cityButtonLayerGroup.addActor(cityButton)
            }

            cityButton!!.update(viewable)
        }
    }

    fun selectCity(city: CityInfo?) : Boolean {
        if (city == null) return false
        return worldScreen.bottomUnitTable.citySelected(city)
    }

    override fun clone(): WorldTileGroup = WorldTileGroup(worldScreen, tileInfo , tileSetStrings)
}
