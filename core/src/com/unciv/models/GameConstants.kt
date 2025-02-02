package com.unciv.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.unciv.json.json
import com.unciv.json.fromJsonFile

object GameConstants {
    // Ленивая загрузка данных из файла gameConstants.json
    private val data: GameConstantsData by lazy {
        val file: FileHandle = Gdx.files.internal("jsons/gameConstants.json")
        json().fromJsonFile(GameConstantsData::class.java, file)
    }

    // Прямой доступ к константам без дополнительных проверок
    val luckProbability: Double get() = data.luckProbability
    val moraleProbability: Double get() = data.moraleProbability
    val armySize: Int get() = data.armySize
}
