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
            unitName = "Spearman",
            amount = 10,
            speed = 3,
            damage = 10,
            maxHealth = 100,
            rangedStrength = 0,
            isSelfFeeding = false,
            currentAmount = 7,
            currentHealth = 80,
            id = 1,
            formation = Formation(current = 25, maximum = 70)
        )

        val serializer = json()
        val restored = serializer.fromJson(Troop::class.java, serializer.toJson(troop))

        assertEquals(25, restored.formation.current)
        assertEquals(70, restored.formation.maximum)
    }

    @Test
    fun `legacy troop without formation starts with formation for surviving soldiers`() {
        val legacyJson = """{
            "unitName":"Spearman",
            "amount":10,
            "speed":3,
            "damage":10,
            "maxHealth":100,
            "rangedStrength":0,
            "isSelfFeeding":false,
            "currentAmount":7,
            "currentHealth":80,
            "id":1
        }"""

        val restored = json().fromJson(Troop::class.java, legacyJson).also {
            it.restoreFormationIfMissing()
        }

        assertEquals(70, restored.formation.current)
        assertEquals(70, restored.formation.maximum)
    }
}
