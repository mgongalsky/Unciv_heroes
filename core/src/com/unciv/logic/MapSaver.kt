package com.unciv.logic

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.TileMap
import com.unciv.ui.saves.Gzip

object MapSaver {

    const val mapsFolder = "maps"
    var saveZipped = true

    private fun getMap(mapName: String) = Gdx.files.local("$mapsFolder/$mapName")

    fun mapFromSavedString(mapString: String, checkSizeErrors: Boolean = true): TileMap {
        val unzippedJson = try {
            Gzip.unzip(mapString.trim())
        } catch (ex: Exception) {
            mapString
        }
        return mapFromJson(unzippedJson).apply {
            if (checkSizeErrors && mapParameters.getArea() != values.size)
                throw UncivShowableException("Invalid map: Area ([${values.size}]) does not match saved dimensions ([${mapParameters.displayMapDimensions()}]).")
            if (!checkSizeErrors)
                mapParameters.mods.filter { '-' in it }.forEach {
                    mapParameters.mods.remove(it)
                    mapParameters.mods.add(it.replace('-', ' '))
                }
        }
    }

    fun mapToSavedString(tileMap: TileMap): String {
        tileMap.assignContinents(TileMap.AssignContinentsMode.Reassign)
        tileMap.mapParameters.mapFormatVersion = MapFormatMigration.currentVersion
        if (tileMap.mapParameters.createdWithVersion.isBlank())
            tileMap.mapParameters.createdWithVersion = UncivGame.VERSION.text
        val mapJson = json().toJson(tileMap)
        return if (saveZipped) Gzip.zip(mapJson) else mapJson
    }

    fun saveMap(mapName: String, tileMap: TileMap) {
        getMap(mapName).writeString(mapToSavedString(tileMap), false)
    }

    fun loadMap(mapFile: FileHandle, checkSizeErrors: Boolean = true): TileMap =
            mapFromSavedString(mapFile.readString(), checkSizeErrors)

    fun getMaps(): Array<FileHandle> = Gdx.files.local(mapsFolder).list()

    private fun mapFromJson(jsonText: String): TileMap {
        val migratedJson = MapFormatMigration.migrate(jsonText)
        return json().fromJson(TileMap::class.java, migratedJson).apply {
            mapParameters.mapFormatVersion = MapFormatMigration.currentVersion
        }
    }

    private class TileMapPreview {
        val mapParameters = MapParameters()
    }

    fun loadMapParameters(mapFile: FileHandle): MapParameters =
            mapParametersFromSavedString(mapFile.readString())

    @Suppress("MemberVisibilityCanBePrivate")
    fun mapParametersFromSavedString(mapString: String): MapParameters {
        val unzippedJson = try {
            Gzip.unzip(mapString.trim())
        } catch (ex: Exception) {
            mapString
        }
        val migratedJson = MapFormatMigration.migrate(unzippedJson)
        return json().fromJson(TileMapPreview::class.java, migratedJson).mapParameters
    }
}
