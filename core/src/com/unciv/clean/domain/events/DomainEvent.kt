package com.unciv.clean.domain.events

import com.badlogic.gdx.math.Vector2

sealed interface DomainEvent {
    data class TurnAdvanced(val turn: Int, val currentPlayer: String) : DomainEvent

    data class EnemyUnitsSpotted(
        val civName: String,
        val positions: List<Vector2>,
        val scope: Scope
    ) : DomainEvent

    data class CitiesCanBombard(
        val civName: String,
        val positions: List<Vector2>
    ) : DomainEvent

    data class ResourcesRevealed(
        val civName: String,
        val resourceName: String,
        val positions: List<Vector2>,
        val nearCity: String? = null
    ) : DomainEvent
}

enum class Scope { IN_TERRITORY, NEAR_TERRITORY }
