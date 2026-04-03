package com.unciv.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.unciv.json.json
import com.unciv.json.fromJsonFile

object GameConstants {
    private var _testData: GameConstantsData? = null

    fun setTestingInstance(data: GameConstantsData) {
        _testData = data
    }

    fun clearTestingInstance() {
        _testData = null
    }

    private val data: GameConstantsData by lazy {
        val file: FileHandle = Gdx.files.internal("jsons/gameConstants.json")
        json().fromJsonFile(GameConstantsData::class.java, file)
    }

    private fun resolveData(): GameConstantsData = _testData ?: data

    val luckProbability: Double get() = resolveData().luckProbability
    val moraleProbability: Double get() = resolveData().moraleProbability
    val armySize: Int get() = resolveData().armySize
}
