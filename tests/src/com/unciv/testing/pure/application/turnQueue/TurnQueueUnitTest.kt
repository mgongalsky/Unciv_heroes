package com.unciv.testing.pure.application.turnQueue

import com.unciv.pure.domain.battle.TurnQueue
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TurnQueueUnitTest {

    private val fastSource = HardcodedTroopDefinitionSource(
        speed = 10,
        damage = 10,
        maxHealth = 100,
        rangedStrength = 0
    )
    private val slowSource = HardcodedTroopDefinitionSource(
        speed = 3,
        damage = 10,
        maxHealth = 100,
        rangedStrength = 0
    )
    private val mediumSource = HardcodedTroopDefinitionSource(
        speed = 5,
        damage = 10,
        maxHealth = 100,
        rangedStrength = 0
    )

    @Before
    fun setUp() {
        TroopFactory.resetIdCounter()
    }

    private fun fast() = TroopFactory.create("Fast", 10, fastSource)
    private fun slow() = TroopFactory.create("Slow", 10, slowSource)
    private fun medium() = TroopFactory.create("Medium", 10, mediumSource)

    @Test
    fun `faster troop goes before slower`() {
        val fast = fast();
        val slow = slow()
        val queue = TurnQueue()
        queue.initialize(listOf(fast), listOf(slow))
        assertEquals(fast, queue.current())
    }

    @Test
    fun `equal speed — attacker goes before defender`() {
        val attacker = medium();
        val defender = medium()
        val queue = TurnQueue()
        queue.initialize(listOf(attacker), listOf(defender))
        assertEquals(attacker, queue.current())
    }

    @Test
    fun `equal-speed troops alternate attacker and defender`() {
        val attackerOne = medium()
        val attackerTwo = medium()
        val defenderOne = medium()
        val defenderTwo = medium()
        val queue = TurnQueue()
        queue.initialize(listOf(attackerOne, attackerTwo), listOf(defenderOne, defenderTwo))
        assertEquals(
            listOf(attackerOne, defenderOne, attackerTwo, defenderTwo),
            queue.getAll()
        )
    }

    @Test
    fun `unpaired equal-speed troops keep their side order at the end`() {
        val attackerOne = medium()
        val attackerTwo = medium()
        val attackerThree = medium()
        val defender = medium()
        val queue = TurnQueue()
        queue.initialize(listOf(attackerOne, attackerTwo, attackerThree), listOf(defender))
        assertEquals(
            listOf(attackerOne, defender, attackerTwo, attackerThree),
            queue.getAll()
        )
    }

    @Test
    fun `advance moves to next troop`() {
        val fast = fast();
        val slow = slow()
        val queue = TurnQueue()
        queue.initialize(listOf(fast), listOf(slow))
        queue.advance()
        assertEquals(slow, queue.current())
    }

    @Test
    fun `visible queue starts with current troop after advance`() {
        val first = fast()
        val second = medium()
        val third = slow()
        val queue = TurnQueue()
        queue.initialize(listOf(first, third), listOf(second))
        queue.advance()
        assertEquals(listOf(second, third, first), queue.getAll())
    }

    @Test
    fun `advance wraps around after last troop`() {
        val fast = fast();
        val slow = slow()
        val queue = TurnQueue()
        queue.initialize(listOf(fast), listOf(slow))
        queue.advance()
        queue.advance()
        assertEquals(fast, queue.current())
    }

    @Test
    fun `remove troop after current — current stays the same`() {
        val fast = fast();
        val slow = slow()
        val queue = TurnQueue()
        queue.initialize(listOf(fast), listOf(slow))
        queue.remove(slow)
        assertEquals(fast, queue.current())
    }

    @Test
    fun `remove troop before current — current stays the same`() {
        val fast = fast();
        val slow = slow()
        val queue = TurnQueue()
        queue.initialize(listOf(fast), listOf(slow))
        queue.advance()
        queue.remove(fast)
        assertEquals(slow, queue.current())
    }

    @Test
    fun `remove all troops — isEmpty and current is null`() {
        val fast = fast();
        val slow = slow()
        val queue = TurnQueue()
        queue.initialize(listOf(fast), listOf(slow))
        queue.remove(fast)
        queue.remove(slow)
        assertTrue(queue.isEmpty())
        assertNull(queue.current())
    }

    @Test
    fun `getAll returns all troops in order`() {
        val fast = fast();
        val slow = slow()
        val queue = TurnQueue()
        queue.initialize(listOf(fast), listOf(slow))
        assertEquals(listOf(fast, slow), queue.getAll())
    }
}
