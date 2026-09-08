package com.unciv.pure.application.battle

/** Deterministic recovery preparation across troop activations, including morale actions. */
object FormationRecoveryUseCase {
    enum class Phase { NONE, WITHDRAWING, WAITING, READY }

    data class State(
        val phase: Phase = Phase.NONE,
        val movementSpent: Float = 0f,
        val interrupted: Boolean = false
    )

    /** Each morale movement may spend up to half of the movement allowance. */
    fun afterMove(
        state: State,
        movementCost: Float,
        maximumMovement: Int,
        hasDamagedFormation: Boolean,
        hasAdjacentEnemy: Boolean
    ): State {
        require(movementCost.isFinite() && movementCost > 0f)
        require(maximumMovement >= 0)
        val shortMove = movementCost.toDouble() * 2.0 <= maximumMovement.toDouble()
        val interrupted = state.interrupted || !shortMove
        val eligible = !interrupted && hasDamagedFormation && !hasAdjacentEnemy
        return state.copy(
            phase = if (eligible) Phase.WITHDRAWING else Phase.NONE,
            movementSpent = state.movementSpent + movementCost,
            interrupted = interrupted
        )
    }

    /** A successful attack or incoming attack cancels preparation, even with zero damage. */
    fun interrupt(state: State): State = state.copy(phase = Phase.NONE, interrupted = true)

    fun endActivation(state: State): State = State(
        phase = if (state.phase == Phase.WITHDRAWING && !state.interrupted)
            Phase.WAITING else Phase.NONE
    )

    fun beginActivation(state: State): State = state.copy(
        phase = if (state.phase == Phase.WAITING) Phase.READY else Phase.NONE,
        movementSpent = 0f,
        interrupted = false
    )

    fun canRestore(state: State, hasDamagedFormation: Boolean, hasAdjacentEnemy: Boolean): Boolean =
        state.phase == Phase.READY && !state.interrupted && state.movementSpent == 0f &&
                hasDamagedFormation && !hasAdjacentEnemy

    fun hasChance(state: State, hasDamagedFormation: Boolean, hasAdjacentEnemy: Boolean): Boolean =
        state.phase != Phase.NONE && !state.interrupted && hasDamagedFormation && !hasAdjacentEnemy
}
