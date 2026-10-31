package com.unciv.testing.victory

import com.unciv.models.ruleset.Victory
import com.unciv.testing.city.CityTestFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Фиксирует прежний выбор победы до подключения сценарных целей и сохраняемого результата. */
class VictoryLegacyCharacterizationTest {
    /** Пустые milestones сейчас считаются выполненными; побеждает первый включённый тип. */
    @Test
    fun firstEnabledVictoryWithEmptyMilestonesWins() {
        CityTestFixture().use { fixture ->
            val first = Victory().apply { name = "CharacterizationFirst" }
            val second = Victory().apply { name = "CharacterizationSecond" }
            fixture.ruleset.victories[first.name] = first
            fixture.ruleset.victories[second.name] = second
            fixture.game.gameParameters.victoryTypes.clear()
            fixture.game.gameParameters.victoryTypes.addAll(listOf(second.name, first.name))
            fixture.civilization.victoryManager.civInfo = fixture.civilization

            assertEquals(second.name, fixture.civilization.victoryManager.getVictoryTypeAchieved())
        }
    }

    /** Неизвестный включённый тип пропускается, а известный выключенный не проверяется. */
    @Test
    fun unknownAndDisabledVictoriesDoNotWin() {
        CityTestFixture().use { fixture ->
            val disabled = Victory().apply { name = "CharacterizationDisabled" }
            fixture.ruleset.victories[disabled.name] = disabled
            fixture.game.gameParameters.victoryTypes.clear()
            fixture.game.gameParameters.victoryTypes.add("CharacterizationMissing")
            fixture.civilization.victoryManager.civInfo = fixture.civilization

            assertNull(fixture.civilization.victoryManager.getVictoryTypeAchieved())
        }
    }

    /** Незавершённое условие первого типа не мешает проверке следующего включённого типа. */
    @Test
    fun incompleteVictoryAllowsNextEnabledVictory() {
        CityTestFixture().use { fixture ->
            val incomplete = Victory().apply {
                name = "CharacterizationIncomplete"
                milestones.add("Win diplomatic vote")
            }
            val complete = Victory().apply { name = "CharacterizationComplete" }
            fixture.ruleset.victories[incomplete.name] = incomplete
            fixture.ruleset.victories[complete.name] = complete
            fixture.game.gameParameters.victoryTypes.clear()
            fixture.game.gameParameters.victoryTypes.addAll(listOf(incomplete.name, complete.name))
            fixture.civilization.victoryManager.civInfo = fixture.civilization

            assertEquals(complete.name, fixture.civilization.victoryManager.getVictoryTypeAchieved())
        }
    }
}
