package com.unciv.pure.application.battle.session

import com.unciv.pure.application.battle.BattleAttackPreview
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleMovementPreviewUseCase
import com.unciv.pure.application.battle.BattleTileAppearanceUseCase
import com.unciv.pure.domain.battle.Point

/** Read-only presentation values captured at the same revision as the battle state. */
data class BattlePresentationSnapshot(
    val tiles: List<BattleTilePresentation> = emptyList(),
    val movementRanges: List<BattleTroopMovementPresentation> = emptyList(),
    val attacks: List<BattleAttackPresentation> = emptyList()
)

data class BattleTilePresentation(
    val position: Point,
    val appearance: BattleTileAppearanceUseCase.Appearance
)

data class BattleMovementTilePresentation(
    val position: Point,
    val style: BattleMovementPreviewUseCase.TileStyle
)

data class BattleTroopMovementPresentation(
    val troopId: Int,
    val tiles: List<BattleMovementTilePresentation>
)

data class BattleAttackPresentation(
    val command: BattleCommand,
    val preview: BattleAttackPreview
)
