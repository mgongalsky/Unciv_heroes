package com.unciv.testing.story

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.story.StoryWorldEffects
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.story.*
import com.unciv.testing.city.CityTestFixture
import org.junit.Assert.*
import org.junit.Test

/** Проверяет подготовку игровых последствий и явную публикацию без UI. */
class StoryWorldEffectsTest {
    /** Подготовка награды не меняет казну; публикация выдаёт заданное количество. */
    @Test
    fun goldPreparationDoesNotMutateTreasury() {
        CityTestFixture().use { fixture ->
            val owner = fixture.civilization
            val before = owner.gold
            val apply = StoryWorldEffects.prepare(fixture.game, PendingStoryEffect(
                1, StoryEffect(StoryEffectKind.GrantGold, amount = 17), StoryEvent(ownerName = owner.civName)
            ))
            assertEquals(before, owner.gold)
            apply()
            assertEquals(before + 17, owner.gold)
        }
    }

    /** Появление армии готовит стек отдельно и публикует его в точной клетке с нулевым движением. */
    @Test
    fun armyAppearsOnlyAfterPublication() {
        CityTestFixture().use { fixture ->
            val apply = StoryWorldEffects.prepare(fixture.game, PendingStoryEffect(
                1, StoryEffect(StoryEffectKind.SpawnArmy, amount = 7, position = CityPosition(0f, 0f), unitName = "Spearman"),
                StoryEvent(ownerName = fixture.civilization.civName)
            ))
            assertNull(fixture.game.tileMap[Vector2.Zero].militaryUnit)
            assertTrue(fixture.civilization.getCivUnits().none())
            apply()
            val army = fixture.civilization.getCivUnits().single()
            assertSame(army, fixture.game.tileMap[Vector2.Zero].militaryUnit)
            assertEquals(7, army.army.getAllTroops().filterNotNull().single().currentAmount)
            assertEquals(0f, army.currentMovement, 0f)
        }
    }

    /** Невозможное размещение не уничтожает существующего юнита и не публикует нового. */
    @Test
    fun occupiedArmyPositionIsRejectedWithoutMutation() {
        CityTestFixture().use { fixture ->
            val original = fixture.civilization.placeUnitNearTile(Vector2.Zero, "Warrior")!!
            try {
                StoryWorldEffects.prepare(fixture.game, PendingStoryEffect(
                    1, StoryEffect(StoryEffectKind.SpawnArmy, amount = 7, position = CityPosition(0f, 0f), unitName = "Spearman"),
                    StoryEvent(ownerName = fixture.civilization.civName)
                ))
                fail("Занятая клетка должна быть отклонена")
            } catch (_: IllegalArgumentException) { }
            assertSame(original, fixture.civilization.getCivUnits().single())
            assertSame(original, fixture.game.tileMap[Vector2.Zero].militaryUnit)
        }
    }
}
