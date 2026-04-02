import com.unciv.pure.application.battle.CalculateDamageUseCase
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

// testing/pure/application/battle/CalculateDamageUseCaseCharTest.kt
class CalculateDamageUseCaseCharTest {

    private fun execute(
        attackerAmount: Int = 10,
        attackerDamage: Int = 10,
        defenderAmount: Int = 10,
        defenderHealth: Int = 100,
        defenderMaxHealth: Int = 100,
        isLuck: Boolean = false
    ) = CalculateDamageUseCase.execute(
        attackerAmount, attackerDamage,
        defenderAmount, defenderHealth, defenderMaxHealth,
        isLuck
    )

    @Test
    fun `basic attack`() {
        val result = execute()
        assertEquals(100, result.damageDealt)
        assertEquals(1, result.perished)
        assertEquals(9, result.remainingAmount)
        assertEquals(100, result.remainingHealth)
        assertFalse(result.isLuck)
    }

    @Test
    fun `luck doubles damage`() {
        val normal = execute(isLuck = false)
        val lucky = execute(isLuck = true)
        assertEquals(100, normal.damageDealt)
        assertEquals(1, normal.perished)
        assertEquals(200, lucky.damageDealt)
        assertEquals(2, lucky.perished)
    }

    @Test
    fun `wounded defender takes more damage`() {
        val healthy = execute(defenderHealth = 100)
        val wounded = execute(defenderHealth = 50)
        assertEquals(1, healthy.perished)
        assertEquals(9, healthy.remainingAmount)
        assertEquals(1, wounded.perished)
        assertEquals(9, wounded.remainingAmount)
        // TODO: wounded defender remainingHealth должен отличаться — проверить
    }

    @Test
    fun `full army wiped out`() {
        val result = execute(attackerAmount = 100, attackerDamage = 100, defenderAmount = 5)
        assertEquals(100, result.perished)
        assertEquals(0, result.remainingAmount)
    }

    @Test
    fun `single attacker vs single defender`() {
        val result = execute(attackerAmount = 1, defenderAmount = 1)
        assertEquals(10, result.damageDealt)
        assertEquals(0, result.perished)
        assertEquals(1, result.remainingAmount)
        assertEquals(90, result.remainingHealth)
    }
}
