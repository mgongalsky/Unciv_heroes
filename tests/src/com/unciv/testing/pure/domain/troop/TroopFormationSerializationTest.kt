package com.unciv.testing.pure.domain.troop

import com.unciv.json.json
import com.unciv.pure.domain.troop.Formation
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.assertEquals
import org.junit.Test

class TroopFormationSerializationTest {
    @Test
    fun `formation damage survives json round trip`() {
        val troop = Troop(
            unitName = "Spearman", amount = 10, speed = 3, damage = 10,
            maxHealth = 100, currentAmount = 7, currentHealth = 80, id = 1,
            formation = Formation(current = 25, maximum = 70),
            formationHealthPercent = 10, formationDamageReductionPercent = 50
        )
        val serializer = json()
        val restored = serializer.fromJson(Troop::class.java, serializer.toJson(troop))
        restored.restoreFormationIfMissing()
        assertEquals(25, restored.formation.current)
        assertEquals(70, restored.formation.maximum)
    }

    @Test
    fun `legacy troop without formation parameters has no formation`() {
        val legacyJson = """{
            "unitName":"Spearman", "amount":10, "speed":3, "damage":10,
            "maxHealth":100, "currentAmount":7, "currentHealth":80, "id":1
        }"""
        val restored = json().fromJson(Troop::class.java, legacyJson)
        restored.restoreFormationIfMissing()
        assertEquals(0, restored.formation.current)
        assertEquals(0, restored.formation.maximum)
    }

    @Test
    fun `legacy formation is removed when parameters are absent`() {
        val restored = json().fromJson(
            Troop::class.java, """{
            "unitName":"Peasant", "amount":10, "maxHealth":5,
            "currentAmount":10, "formation":{"current":80,"maximum":100}
        }"""
        )
        restored.restoreFormationIfMissing()
        assertEquals(0, restored.formation.current)
        assertEquals(0, restored.formation.maximum)
    }
}
