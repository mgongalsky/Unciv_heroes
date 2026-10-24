package com.unciv.ui.battlescreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.Pixmap
import com.unciv.logic.Direction
import com.unciv.pure.application.battle.BattleAttackPreview
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.battle.RangedShotRules

/** Owns the existing battle cursor assets for one screen. */
class BattleCursorSet {
    val move = load("BattleMoveCursor128.png", 32, 64)
    val shoot = load("BattleArrowCursor128.png", 64, 64)
    val indirectShoot = load("BattleBrokenArrowCursor128.png", 64, 64)
    val cancel = load("BattleCancelCursor64.png", 32, 32)
    val attacks = (0..5).map { load("BattleAttackCursor$it.png", 64, if (it == 1 || it == 4) 16 else 64) }

    fun show(command: BattleCommand?, direction: Direction, preview: BattleAttackPreview?) {
        val cursor = when (command) {
            is BattleCommand.Move -> move
            is BattleCommand.Shoot -> if (preview?.rangedShot?.shotType == RangedShotRules.ShotType.INDIRECT)
                indirectShoot else shoot
            is BattleCommand.Attack -> attacks.getOrNull(direction.num) ?: cancel
            else -> cancel
        }
        Gdx.graphics.setCursor(cursor)
    }

    fun dispose() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow)
        (attacks + listOf(move, shoot, indirectShoot, cancel)).forEach { it.dispose() }
    }

    companion object {
        fun load(filename: String, xHotspot: Int, yHotspot: Int): Cursor {
            val pixmap = Pixmap(Gdx.files.internal("ExtraImages/$filename"))
            return try { Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot) }
            finally { pixmap.dispose() }
        }
    }
}
