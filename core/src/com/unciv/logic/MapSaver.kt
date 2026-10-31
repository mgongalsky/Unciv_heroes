package com.unciv.logic

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.TileMap
import com.unciv.ui.saves.Gzip

/** Читает и записывает исходные карты; сценарные описания сохраняются без создания игровых городов. */
object MapSaver {

    const val mapsFolder = "maps"
    var saveZipped = true

    private fun getMap(mapName: String) = Gdx.files.local("$mapsFolder/$mapName")

    /** Поддерживает обычный JSON и сжатую строку; проверку старых размеров можно отключить явно. */
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

    /** Проверяет ссылки сценария до записи и выставляет текущую версию формата карты. */
    fun mapToSavedString(tileMap: TileMap): String {
        com.unciv.logic.map.ScenarioMapValidation.validateStructure(tileMap)
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

    /** Миграция предшествует десериализации; ошибки сценария превращаются в сообщение для пользователя. */
    private fun mapFromJson(jsonText: String): TileMap {
        val migratedJson = MapFormatMigration.migrate(jsonText)
        return json().fromJson(TileMap::class.java, migratedJson).apply {
            mapParameters.mapFormatVersion = MapFormatMigration.currentVersion
            try {
                com.unciv.logic.map.ScenarioMapValidation.validateStructure(this)
            } catch (invalid: IllegalArgumentException) {
                throw UncivShowableException("Invalid scenario: ${invalid.message}")
            }
        }
    }

    /** Небольшая проекция JSON для меню запуска; остальные поля карты намеренно не читаются. */
    private class ScenarioSlotsPreview {
        val scenarioPlayerSlots = arrayListOf<com.unciv.pure.domain.city.ScenarioPlayerSlot>()
    }

    /** Читает только слоты участников, без создания клеток, юнитов и игровых городов. */
    fun loadScenarioPlayerSlots(mapFile: FileHandle): List<com.unciv.pure.domain.city.ScenarioPlayerSlot> {
        val saved = mapFile.readString()
        val plain = try { Gzip.unzip(saved.trim()) } catch (_: Exception) { saved }
        val migrated = MapFormatMigration.migrate(plain)
        return json().fromJson(ScenarioSlotsPreview::class.java, migrated).scenarioPlayerSlots
    }

    private class TileMapPreview {
        val mapParameters = MapParameters()
    }

    fun loadMapParameters(mapFile: FileHandle): MapParameters =
            mapParametersFromSavedString(mapFile.readString())

    /** Превью использует ту же миграцию, что полная загрузка, чтобы одинаково понимать старые карты. */
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
