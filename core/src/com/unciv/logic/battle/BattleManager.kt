package com.unciv.logic.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.infrastructure.battle.RealBattleRandom
import com.unciv.logic.HexMath
import com.unciv.models.GameConstants
import com.unciv.pure.application.battle.*
import com.unciv.pure.application.pathfinding.MovementRangeUseCase
import com.unciv.pure.application.pathfinding.TroopMovementContext
import com.unciv.pure.domain.army.IArmy
import com.unciv.pure.domain.battle.IBattleField
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.TurnQueue
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop

/** Shared combat rules. Armies and battlefield expose only combat data. */
open class BattleManager(
    private var attackerArmy: IArmy,
    private var defenderArmy: IArmy,
    val battleField: IBattleField,
    private val random: IBattleRandom = RealBattleRandom(),
    private val moraleProbability: Double = GameConstants.moraleProbability,
    private val luckProbability: Double = GameConstants.luckProbability
) {
    internal var effectProbabilities: BattleEffectProbabilities? = null
    private val remainingRetaliationDamageByTroopId = mutableMapOf<Int, Int>()
    protected val troopPositions = mutableMapOf<Troop, IBattleTile>()
    private val turnQueue = TurnQueue()
    private val verboseAttack = java.lang.Boolean.getBoolean("battle.verbose")

    fun initializeBattle() {
        val attackerTroops = attackerArmy.getAllTroops().filterNotNull()
        val defenderTroops = defenderArmy.getAllTroops().filterNotNull()
        fun startingPosition(x: Float, index: Int, count: Int): Vector2 {
            val y = if (count == 5) listOf(3f, 1f, 0f, -1f, -3f)[index]
            else 3f - index.toFloat() * 2
            return HexMath.evenQ2HexCoords(Vector2(x, y))
        }
        attackerTroops.forEachIndexed { index, troop ->
            val tile = battleField.getTileAt(
                startingPosition(
                    -7f,
                    index,
                    attackerTroops.size
                )
            ) as? IBattleTile
                ?: return@forEachIndexed
            troopPositions[troop] = tile
            tile.receiveTroop(troop)
        }
        defenderTroops.forEachIndexed { index, troop ->
            val tile = battleField.getTileAt(
                startingPosition(
                    6f,
                    index,
                    defenderTroops.size
                )
            ) as? IBattleTile
                ?: return@forEachIndexed
            troopPositions[troop] = tile
            tile.receiveTroop(troop)
        }
        initializeTurnQueue()
    }

    fun initializeTurnQueue() {
        turnQueue.initialize(
            attackerArmy.getAllTroops().filterNotNull(),
            defenderArmy.getAllTroops().filterNotNull()
        )
    }

    fun getTroopTile(troop: Troop): IBattleTile? = troopPositions[troop]
    protected open fun getTroopCurrentTile(troop: Troop): IBattleTile? = troopPositions[troop]
    protected open fun moveTroop(troop: Troop, targetTile: IBattleTile) {
        troopPositions[troop]?.clearTroop()
        troopPositions[troop] = targetTile
        targetTile.receiveTroop(troop)
    }

    fun getCurrentTroop(): Troop? = turnQueue.current()
    private fun getArmyOf(troop: Troop): IArmy? = when {
        attackerArmy.contains(troop) -> attackerArmy
        defenderArmy.contains(troop) -> defenderArmy
        else -> null
    }

    private fun isMoraleTriggered(troop: Troop) = IsMoraleTriggeredUseCase.execute(
        moraleValue = getArmyOf(troop)?.getBattleMorale() ?: 0,
        random = random,
        moraleProbability = configuredMoraleProbability(moraleProbability)
    )

    private fun isLuckTriggered(troop: Troop): Boolean {
        val troopLuck = getArmyOf(troop)?.getBattleLuck() ?: 1
        val configuredProbability = configuredLuckProbability(luckProbability)
        val effectiveProbability = if (troopLuck <= 3) (configuredProbability / 3.0) * troopLuck
        else configuredProbability
        return random.nextDouble() < effectiveProbability
    }

    /** Post-battle recovery, separate from command execution. */
    fun finishBattle() {
        attackerArmy.finishBattle()
        defenderArmy.finishBattle()
    }

    private fun isAttackerWinner(): Boolean? {
        val attackerAlive = attackerArmy.getAllTroops().any { (it?.currentAmount ?: 0) > 0 }
        val defenderAlive = defenderArmy.getAllTroops().any { (it?.currentAmount ?: 0) > 0 }
        return when {
            attackerAlive && !defenderAlive -> true
            defenderAlive && !attackerAlive -> false
            else -> null
        }
    }

    fun getBattleResult(): BattleTotalResult? {
        val remainingAttackers =
                attackerArmy.getAllTroops().filterNotNull().filter { it.currentAmount > 0 }
        val remainingDefenders =
                defenderArmy.getAllTroops().filterNotNull().filter { it.currentAmount > 0 }
        return when {
            remainingAttackers.isNotEmpty() && remainingDefenders.isEmpty() -> BattleTotalResult(
                attackerArmy
            )

            remainingDefenders.isNotEmpty() && remainingAttackers.isEmpty() -> BattleTotalResult(
                defenderArmy
            )

            remainingAttackers.isEmpty() && remainingDefenders.isEmpty() -> BattleTotalResult(null)
            else -> null
        }
    }

    fun getTroopOnTile(tile: IBattleTile): Troop? =
            troopPositions.entries.find { it.value == tile }?.key

    fun setTroopPosition(troop: Troop, tile: IBattleTile) {
        troopPositions[troop] = tile
    }

    fun getTroopById(id: Int): Troop? = (attackerArmy.getAllTroops() + defenderArmy.getAllTroops())
        .filterNotNull().find { it.id == id }

    fun isBattleOn(): Boolean =
            attackerArmy.getAllTroops().any { (it?.currentAmount ?: 0) > 0 } &&
                    defenderArmy.getAllTroops().any { (it?.currentAmount ?: 0) > 0 }

    fun isTileOccupiedByAlly(troop: Troop, targetTile: IBattleTile): Boolean {
        val targetTroop = targetTile.getTroop() ?: return false
        val isAlly = if (attackerArmy.contains(troop)) attackerArmy.contains(targetTroop)
        else defenderArmy.contains(targetTroop)
        return isAlly && targetTroop != troop
    }

    fun isTileOccupiedByEnemy(troop: Troop, targetTile: IBattleTile): Boolean {
        val targetTroop = targetTile.getTroop() ?: return false
        return if (attackerArmy.contains(troop)) defenderArmy.contains(targetTroop)
        else attackerArmy.contains(targetTroop)
    }

    fun isTileFree(targetTile: INavigableTile): Boolean =
            (targetTile as? IBattleTile)?.getTroop() == null

    fun getEnemies(troop: Troop): List<Troop> = when {
        attackerArmy.contains(troop) -> defenderArmy.getAllTroops().filterNotNull()
        defenderArmy.contains(troop) -> attackerArmy.getAllTroops().filterNotNull()
        else -> emptyList()
    }

    private fun enemyChecker(troop: Troop): (Troop) -> Boolean = { other ->
        attackerArmy.contains(troop) && defenderArmy.contains(other) ||
                defenderArmy.contains(troop) && attackerArmy.contains(other)
    }

    fun getReachableTiles(troop: Troop): List<IBattleTile> {
        val currentTile = getTroopTile(troop) ?: return emptyList()
        return MovementRangeUseCase.execute(
            startTile = currentTile, unitMovement = troop.speed.toFloat(),
            context = TroopMovementContext(troop, enemyChecker(troop))
        ).keys.toList()
    }

    fun isTileAchievable(troop: Troop, targetTile: INavigableTile): Boolean =
            battleField.contains(targetTile) && isReachableInCurrentTurn(troop, targetTile)

    fun getAttackerArmy() = attackerArmy
    fun getDefenderArmy() = defenderArmy

    fun attack(defender: Troop, attacker: Troop? = getCurrentTroop()): Boolean {
        if (attacker == null) return false
        val isLuck = isLuckTriggered(attacker)
        val incomingDamage = attacker.currentAmount * attacker.damage * if (isLuck) 2 else 1
        val formationDamage = ApplyFormationDamageUseCase.execute(
            ApplyFormationDamageUseCase.Input(
                incomingDamage,
                if (defender.hasFormation) defender.formation.current else 0,
                defender.formationDamageReductionPercent.coerceIn(0, 100), 100
            )
        )
        val result = CalculateDamageUseCase.execute(
            attackerAmount = 1, attackerDamage = formationDamage.damageToSoldiers,
            defenderAmount = defender.currentAmount, defenderHealth = defender.currentHealth,
            defenderMaxHealth = defender.maxHealth, isLuck = false
        ).copy(isLuck = isLuck)
        defender.formation.current = formationDamage.remainingFormation
        defender.currentAmount = result.remainingAmount
        defender.currentHealth = result.remainingHealth
        if (defender.currentAmount <= 0) perishTroop(defender)
        return result.isLuck
    }

    fun perishTroop(troop: Troop) {
        removeTroop(troop)
        if (verboseAttack) println("Troop ${troop.unitName} has perished.")
    }

    fun removeTroop(troop: Troop) {
        turnQueue.remove(troop)
        troopPositions.remove(troop)?.clearTroop()
        if (attackerArmy.contains(troop)) attackerArmy.removeTroop(troop)
        else if (defenderArmy.contains(troop)) defenderArmy.removeTroop(troop)
    }

    fun advanceTurn() {
        if (turnQueue.isEmpty()) {
            finishBattle()
            return
        }
        turnQueue.advance()
        turnQueue.current()?.let { remainingRetaliationDamageByTroopId.remove(it.id) }
    }

    fun canShoot(troop: Troop): Boolean = troop.rangedStrength != 0
    fun getTurnQueue(): List<Troop> = turnQueue.getAll()
    protected open fun isReachableInCurrentTurn(troop: Troop, targetTile: INavigableTile): Boolean {
        val currentTile = getTroopTile(troop) ?: return false
        val target = targetTile as? IBattleTile ?: return false
        return MovementRangeUseCase.execute(
            startTile = currentTile, unitMovement = troop.speed.toFloat(),
            context = TroopMovementContext(troop, enemyChecker(troop)), targetTile = target
        ).containsKey(target)
    }

    private fun performMoveAction(
        troop: Troop, targetPosition: IBattleTile, isMorale: Boolean,
        onApplicationEvent: ((BattleEvent) -> Unit)? = null
    ): BattleCommandResult {
        val currentTile = getTroopCurrentTile(troop)
        val output = PerformMoveUseCase.execute(
            PerformMoveUseCase.Input(
                targetPosition.toPoint(),
                currentTile?.toPoint(),
                isTileAchievable(troop, targetPosition),
                isTileOccupiedByAlly(troop, targetPosition),
                isTileFree(targetPosition)
            )
        )
        if (output.success) {
            val movementDistance =
                    HexMath.getDistance(currentTile!!.position, targetPosition.position)
            val formation = UpdateFormationAfterMoveUseCase.execute(
                UpdateFormationAfterMoveUseCase.Input(
                    currentFormation = troop.formation.current,
                    maximumFormation = troop.formation.maximum,
                    movementDistance = movementDistance,
                    maximumMovement = troop.speed,
                    turnEndsWithoutAttack = !isMorale
                )
            )
            troop.formation.current = formation.remainingFormation
            moveTroop(troop, targetPosition)
            publishApplicationEvent(
                BattleEvent.TroopMoved(
                    troop.id,
                    output.movedFrom!!,
                    output.movedTo!!,
                    isMorale
                ), onApplicationEvent
            )
        }
        if (!isBattleOn()) publishApplicationEvent(
            BattleEvent.BattleEnded(isAttackerWinner()),
            onApplicationEvent
        )
        return BattleCommandResult(
            success = output.success,
            movedFrom = output.movedFrom,
            movedTo = output.movedTo,
            rejection = output.rejection,
            isMorale = isMorale,
            battleEnded = !isBattleOn()
        )
    }

    internal fun performMoveCommand(
        troop: Troop, targetPosition: IBattleTile,
        onApplicationEvent: ((BattleEvent) -> Unit)? = null
    ): BattleCommandResult =
            performMoveAction(troop, targetPosition, isMoraleTriggered(troop), onApplicationEvent)

    private fun performSkipAction(
        troop: Troop,
        onApplicationEvent: ((BattleEvent) -> Unit)? = null
    ): BattleCommandResult {
        troop.formation.current = RestoreFormationUseCase.execute(
            RestoreFormationUseCase.Input(
                currentFormation = troop.formation.current,
                maximumFormation = troop.formation.maximum
            )
        )
        publishApplicationEvent(BattleEvent.TurnSkipped, onApplicationEvent)
        if (!isBattleOn()) publishApplicationEvent(
            BattleEvent.BattleEnded(isAttackerWinner()),
            onApplicationEvent
        )
        return BattleCommandResult(success = true, isMorale = false, battleEnded = !isBattleOn())
    }

    internal fun performSkipCommand(
        troop: Troop,
        onApplicationEvent: ((BattleEvent) -> Unit)? = null
    ): BattleCommandResult {
        // Preserve the game's random draw even though Skip does not grant an extra action.
        isMoraleTriggered(troop)
        return performSkipAction(troop, onApplicationEvent)
    }

    private fun performAttackAction(
        troop: Troop, targetPosition: IBattleTile, attackTile: IBattleTile?,
        isMorale: Boolean, onApplicationEvent: ((BattleEvent) -> Unit)? = null
    ): BattleCommandResult {
        val defender = getTroopOnTile(targetPosition)
        val currentTile = getTroopCurrentTile(troop)
        val targetIsEnemy = defender != null && isTileOccupiedByEnemy(troop, targetPosition)
        val attackFromAchievable = attackTile != null && isTileAchievable(troop, attackTile)
        val attackFromFree = attackTile != null && isTileFree(attackTile)
        val canAttack = defender != null && attackTile != null && targetIsEnemy &&
                attackFromAchievable && (attackFromFree || currentTile == attackTile)
        val attackerIsLuck: Boolean
        val remaining: Int
        val died: Boolean
        if (canAttack) {
            moveTroop(troop, attackTile!!)
            attackerIsLuck = isLuckTriggered(troop)
            val defenderIsLuck = isLuckTriggered(defender!!)
            fun snapshot(unit: Troop) = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
                unit.currentAmount,
                unit.damage,
                unit.currentHealth,
                unit.maxHealth,
                if (unit.hasFormation) unit.formation.current else 0,
                unit.formationDamageReductionPercent
            )

            val exchange = CalculateFormationMeleeExchangeUseCase.execute(
                attacker = snapshot(troop), defender = snapshot(defender),
                attackerIsLuck = attackerIsLuck, defenderIsLuck = defenderIsLuck,
                defenderRetaliationDamage = remainingRetaliationDamageByTroopId[defender.id]
            )
            remainingRetaliationDamageByTroopId[defender.id] = exchange.remainingRetaliationDamage
            troop.formation.current = exchange.attackerRemainingFormation
            defender.formation.current = exchange.defenderRemainingFormation
            troop.currentAmount = exchange.damageToAttacker.remainingAmount
            troop.currentHealth = exchange.damageToAttacker.remainingHealth
            defender.currentAmount = exchange.damageToDefender.remainingAmount
            defender.currentHealth = exchange.damageToDefender.remainingHealth
            remaining = defender.currentAmount
            died = defender.currentAmount <= 0
            if (troop.currentAmount <= 0) perishTroop(troop)
            if (defender.currentAmount <= 0) perishTroop(defender)
        } else {
            attackerIsLuck = false
            remaining = 0
            died = false
        }
        val output = PerformAttackUseCase.execute(
            PerformAttackUseCase.Input(
                troop.id,
                defender?.id,
                attackTile?.toPoint(),
                currentTile?.toPoint(),
                targetIsEnemy,
                attackFromAchievable,
                attackFromFree,
                attackerIsLuck,
                isMorale,
                remaining,
                died
            )
        )
        if (output.success) publishApplicationEvent(
            BattleEvent.TroopAttacked(
                troop.id,
                defender!!.id,
                remaining,
                output.isLuck,
                output.isMorale,
                died
            ), onApplicationEvent
        )
        if (!isBattleOn()) publishApplicationEvent(
            BattleEvent.BattleEnded(isAttackerWinner()),
            onApplicationEvent
        )
        return BattleCommandResult(
            success = output.success, movedFrom = output.movedFrom,
            movedTo = output.movedTo, rejection = output.rejection, isLuck = output.isLuck,
            isMorale = output.isMorale, battleEnded = !isBattleOn()
        )
    }

    internal fun performAttackCommand(
        troop: Troop, targetPosition: IBattleTile, attackTile: IBattleTile?,
        onApplicationEvent: ((BattleEvent) -> Unit)? = null
    ): BattleCommandResult =
            performAttackAction(
                troop,
                targetPosition,
                attackTile,
                isMoraleTriggered(troop),
                onApplicationEvent
            )

    private fun performShootAction(
        troop: Troop, targetPosition: IBattleTile, isMorale: Boolean,
        onApplicationEvent: ((BattleEvent) -> Unit)? = null
    ): BattleCommandResult {
        val defender = getTroopOnTile(targetPosition)
        val canShoot = canShoot(troop)
        val targetIsEnemy = defender != null && isTileOccupiedByEnemy(troop, targetPosition)
        val isLuck: Boolean
        val remaining: Int
        val died: Boolean
        if (defender != null && canShoot && targetIsEnemy) {
            isLuck = attack(defender, troop)
            remaining = defender.currentAmount
            died = defender.currentAmount <= 0
            if (died) removeTroop(defender)
        } else {
            isLuck = false
            remaining = 0
            died = false
        }
        val output = PerformShootUseCase.execute(
            PerformShootUseCase.Input(
                troop.id, defender?.id, canShoot, targetIsEnemy, isLuck, isMorale, remaining, died
            )
        )
        if (output.success) publishApplicationEvent(
            BattleEvent.TroopShot(
                troop.id,
                defender!!.id,
                remaining,
                output.isLuck,
                output.isMorale,
                died
            ), onApplicationEvent
        )
        if (!isBattleOn()) publishApplicationEvent(
            BattleEvent.BattleEnded(isAttackerWinner()),
            onApplicationEvent
        )
        return BattleCommandResult(
            success = output.success, rejection = output.rejection,
            isLuck = output.isLuck, isMorale = output.isMorale, battleEnded = !isBattleOn()
        )
    }

    internal fun performShootCommand(
        troop: Troop, targetPosition: IBattleTile,
        onApplicationEvent: ((BattleEvent) -> Unit)? = null
    ): BattleCommandResult =
            performShootAction(troop, targetPosition, isMoraleTriggered(troop), onApplicationEvent)

    private fun publishApplicationEvent(
        event: BattleEvent,
        onApplicationEvent: ((BattleEvent) -> Unit)? = null
    ) {
        onApplicationEvent?.invoke(event)
    }
    fun hasRetaliationRemaining(troop: Troop): Boolean =
            remainingRetaliationDamageByTroopId[troop.id] != 0

    /** Shared activation completion for interactive and automated callers.
     * Rejected player commands are retried before calling this method.
     */
    fun completeAction(actionResult: BattleCommandResult?) {
        if (!isBattleOn() || getCurrentTroop() == null || getTurnQueue().isEmpty()) return
        if (actionResult?.success != true || ShouldAdvanceTurnUseCase.execute(actionResult)) {
            advanceTurn()
        }
    }
}
