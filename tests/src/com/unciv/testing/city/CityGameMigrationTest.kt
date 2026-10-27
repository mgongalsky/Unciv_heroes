package com.unciv.testing.city

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.UncivFiles
import com.unciv.logic.civilization.CivilizationInfo
import org.junit.Assert.*
import org.junit.Test

class CityGameMigrationTest {
    private fun text(tree: JsonValue) = tree.toJson(JsonWriter.OutputType.json)
    private fun duplicate(tree: JsonValue) = JsonReader().parse(text(tree))

    private fun withSave(action: (CityTestFixture, JsonValue) -> Unit) {
        CityTestFixture().use { fixture ->
            val previous = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                val city = fixture.foundCity()
                city.previousOwner = "Greece"
                city.population.foodStored = 3
                city.currConscription = 17
                action(fixture, JsonReader().parse(json().toJson(fixture.game.clone())))
            } finally {
                CivilizationInfo.monsterGameInfo = previous
            }
        }
    }

    @Test
    fun canonicalSaveWritesCitiesOnceAndCloneRoundTrips() = withSave { fixture, tree ->
        assertEquals(5, tree.get("version").getInt("number"))
        assertEquals(1, tree.get("cities").size)
        assertTrue(tree.get("civilizations").all { !it.has("cities") })
        val entry = tree.get("cities").get(0)
        assertEquals("Player", entry.get("ownership").getString("type"))
        assertEquals("Rome", entry.get("ownership").getString("civName"))
        val loaded = UncivFiles.gameInfoFromString(text(tree))
        val copy = loaded.clone()
        val restored = UncivFiles.gameInfoFromString(UncivFiles.gameInfoToString(copy, false))
        assertNotSame(loaded.getCities().single(), restored.getCities().single())
        assertEquals(fixture.game.getCities().single().id, restored.getCities().single().id)
        assertEquals("Greece", restored.getCities().single().previousOwner)
        assertSame(restored.getCities().single(), restored.civilizations.single().cities.single())
    }

    @Test
    fun legacyCivilizationContainerMigratesAndSurvivesAnotherSaveAndTurn() = withSave { fixture, tree ->
        val oldCities = JsonValue(JsonValue.ValueType.array)
        oldCities.addChild(duplicate(tree.get("cities").get(0).get("city")))
        tree.remove("cities")
        tree.get("civilizations").get(0).addChild("cities", oldCities)
        tree.get("version").apply { remove("number"); addChild("number", JsonValue(4L)) }
        val loaded = UncivFiles.gameInfoFromString(text(tree))
        val city = loaded.getCities().single()
        assertEquals(fixture.game.getCities().single().id, city.id)
        assertEquals("Greece", city.previousOwner)
        assertEquals(3, city.population.foodStored)
        assertEquals(17, city.currConscription)
        assertSame(loaded.civilizations.single(), city.garrisonInfo.civInfo)
        val rewritten = JsonReader().parse(UncivFiles.gameInfoToString(loaded, false))
        assertEquals(5, rewritten.get("version").getInt("number"))
        assertFalse(rewritten.get("civilizations").get(0).has("cities"))
        assertEquals(1, rewritten.get("cities").size)
        val reloaded = UncivFiles.gameInfoFromString(text(rewritten))
        val turn = reloaded.turns
        reloaded.nextTurn()
        assertEquals(turn + 1, reloaded.turns)
        assertEquals(city.id, reloaded.getCities().single().id)
    }

    @Test
    fun corruptCanonicalSavesAreRejectedWithSpecificDiagnostics() = withSave { _, source ->
        val cases = listOf<Pair<String, (JsonValue) -> Unit>>(
            "Duplicate city ID" to { tree ->
                tree.get("cities").addChild(duplicate(tree.get("cities").get(0)))
            },
            "Duplicate city position" to { tree ->
                val entry = duplicate(tree.get("cities").get(0))
                entry.get("city").get("id").set("another-city")
                tree.get("cities").addChild(entry)
            },
            "unknown owner" to { tree ->
                tree.get("cities").get(0).get("ownership").get("civName").set("Missing")
            },
            "mixes canonical" to { tree ->
                tree.get("civilizations").get(0).addChild("cities", JsonValue(JsonValue.ValueType.array))
            },
            "has no cities collection" to { tree -> tree.remove("cities"); Unit },
            "not yet supported" to { tree ->
                val owner = tree.get("cities").get(0).get("ownership")
                owner.get("type").set("Neutral")
                owner.remove("civName")
                Unit
            },
            "Unsupported game save version" to { tree -> tree.get("version").apply { remove("number"); addChild("number", JsonValue(6L)) } }
        )
        for ((diagnostic, corrupt) in cases) {
            val tree = duplicate(source)
            corrupt(tree)
            val failure = try {
                json().fromJson(GameInfo::class.java, text(tree))
                null
            } catch (error: Exception) {
                error
            }
            assertNotNull("Must reject: $diagnostic", failure)
            val messages = generateSequence(failure as Throwable?) { it.cause }
                .joinToString("\n") { it.message.orEmpty() }
            assertTrue("Expected $diagnostic, got $messages", messages.contains(diagnostic))
        }
    }
}
