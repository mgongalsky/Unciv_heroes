package com.unciv.logic.city

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.pure.domain.city.CityOwnership

/**
 * Запись единого массива cities в сохранении игры: явное владение плюс обычные данные CityInfo.
 * Владелец не выводится из расположения города внутри списка цивилизации, как в старом формате.
 */
class CitySaveEntry() : IsPartOfGameInfoSerialization, Json.Serializable {
    lateinit var city: CityInfo
        private set

    constructor(city: CityInfo) : this() {
        this.city = city
    }

    /** Не записывает transient-ссылки; Player обозначается civName, Neutral — отдельным типом. */
    override fun write(json: Json) {
        val owner = requireNotNull(city.state.ownership) { "City ${city.id} has no saved owner" }
        json.writeObjectStart("ownership")
        when (owner) {
            is CityOwnership.Player -> {
                require(owner.playerId.isNotBlank()) { "City ${city.id} has an empty owner" }
                json.writeValue("type", "Player")
                json.writeValue("civName", owner.playerId)
            }
            CityOwnership.Neutral -> json.writeValue("type", "Neutral")
        }
        json.writeObjectEnd()
        json.writeValue("city", city, CityInfo::class.java)
    }

    /** Проверяет оболочку до чтения города; ссылки на мир и реального владельца восстанавливаются позже. */
    override fun read(json: Json, jsonData: JsonValue) {
        val payload = requireNotNull(jsonData.get("city")) { "Missing city payload" }
        require(payload.isObject) { "City payload must be an object" }
        val id = payload.getString("id", "")
        require(id.isNotBlank()) { "Missing city ID" }
        require(payload.get("location")?.isObject == true) { "City $id has no location" }
        val ownership = requireNotNull(jsonData.get("ownership")) { "City $id has no ownership" }
        require(ownership.isObject) { "City $id ownership must be an object" }
        val owner = when (val type = ownership.getString("type", "")) {
            "Player" -> {
                val civName = ownership.getString("civName", "")
                require(civName.isNotBlank()) { "City $id has an empty owner" }
                CityOwnership.Player(civName)
            }
            "Neutral" -> {
                // Запрещаем противоречивую запись: нейтральный город не имеет цивилизации-владельца.
                require(!ownership.has("civName")) { "Neutral city $id cannot name a civilization" }
                CityOwnership.Neutral
            }
            else -> throw IllegalArgumentException("City $id has unknown ownership type: $type")
        }
        city = json.readValue(CityInfo::class.java, payload)
        city.state.ownership = owner
    }
}
