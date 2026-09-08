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
        formationRecovery.clear()
        moraleExtraActionTroops.clear()
        pendingFollowUpShots.clear()
        moveAndShootGranted.clear()
        deferredMoveMorale.clear()
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
        if (hasPendingFollowUpShot(troop)) return emptyList()
        val currentTile = getTroopTile(troop) ?: return emptyList()
        return MovementRangeUseCase.execute(
            startTile = currentTile, unitMovement = troop.speed.toFloat(),
            context = TroopMovementContext(troop, enemyChecker(troop))
        ).keys.toList()
    }

    fun isTileAchievable(troop: Troop, targetTile: INavigableTile): Boolean =
            !hasPendingFollowUpShot(troop) && battleField.contains(targetTile) &&
                    isReachableInCurrentTurn(troop, targetTile)

    fun getAttackerArmy() = attackerArmy
    fun getDefenderArmy() = defenderArmy

    fun attack(defender: Troop, attacker: Troop? = getCurrentTroop()): Boolean {
        if (attacker == null) return false
        val isLuck = isLuckTriggered(attacker)
        val incomingDamage = MoveAndShootRules.shotDamage(
            attacker.currentAmount * attacker.damage * if (isLuck) 2 else 1,
            afterMovement = attacker.isRanged && attacker in moveAndShootGranted
        )
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
        pendingFollowUpShots.remove(troop)
        moveAndShootGranted.remove(troop)
        deferredMoveMorale.remove(troop)
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
        turnQueue.current()?.let { troop ->
            formationRecovery[troop] = FormationRecoveryUseCase.endActivation(recoveryState(troop))
            moraleExtraActionTroops.remove(troop)
            pendingFollowUpShots.remove(troop)
            moveAndShootGranted.remove(troop)
            deferredMoveMorale.remove(troop)
            logFormationRecovery(troop, "end-activation")
        }
        turnQueue.advance()
        turnQueue.current()?.let { troop ->
            remainingRetaliationDamageByTroopId.remove(troop.id)
            formationRecovery[troop] =
                    FormationRecoveryUseCase.beginActivation(recoveryState(troop))
            moraleExtraActionTroops.remove(troop)
            pendingFollowUpShots.remove(troop)
            moveAndShootGranted.remove(troop)
            deferredMoveMorale.remove(troop)
            logFormationRecovery(troop, "begin-activation")
        }
    }

    fun canShoot(troop: Troop): Boolean = RangedCombatRules.canShoot(
        isRanged = troop.isRanged,
        isAlive = troop.currentAmount > 0,
        isPlaced = getTroopTile(troop) != null,
        hasAdjacentEnemy = hasAdjacentEnemy(troop)
    )
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
                targetPosition.toPoint(), currentTile?.toPoint(),
                isTileAchievable(troop, targetPosition),
                isTileOccupiedByAlly(troop, targetPosition), isTileFree(targetPosition)
            )
        )
        var grantsShot = false
        if (output.success) {
            val movementCost = MovementRangeUseCase.execute(
                startTile = currentTile!!, unitMovement = troop.speed.toFloat(),
                context = TroopMovementContext(troop, enemyChecker(troop)),
                targetTile = targetPosition
            )[targetPosition]?.totalDistance
            val movementDistance =
                    HexMath.getDistance(currentTile.position, targetPosition.position)
            grantsShot = MoveAndShootRules.grantsShot(
                troop.isRanged, movementDistance, troop.speed, troop in moveAndShootGranted
            )
            val formation = UpdateFormationAfterMoveUseCase.execute(
                UpdateFormationAfterMoveUseCase.Input(
                    currentFormation = troop.formation.current,
                    maximumFormation = troop.formation.maximum,
                    movementDistance = movementDistance,
                    maximumMovement = troop.speed,
                    turnEndsWithoutAttack = !isMorale && !grantsShot
                )
            )
            troop.formation.current = formation.remainingFormation
            moveTroop(troop, targetPosition)
            if (grantsShot) {
                pendingFollowUpShots.add(troop)
                moveAndShootGranted.add(troop)
                if (isMorale) deferredMoveMorale.add(troop)
            }
            formationRecovery[troop] = if (movementCost != null && movementCost > 0f) {
                FormationRecoveryUseCase.afterMove(
                    recoveryState(troop), movementCost, troop.speed,
                    hasDamagedFormation(troop), hasAdjacentEnemy(troop)
                )
            } else FormationRecoveryUseCase.interrupt(recoveryState(troop))
            publishApplicationEvent(
                BattleEvent.TroopMoved(troop.id, output.movedFrom!!, output.movedTo!!, isMorale),
                onApplicationEvent
            )
        }
        if (!isBattleOn()) publishApplicationEvent(
            BattleEvent.BattleEnded(isAttackerWinner()), onApplicationEvent
        )
        return BattleCommandResult(
            success = output.success, movedFrom = output.movedFrom, movedTo = output.movedTo,
            rejection = output.rejection, isMorale = isMorale, battleEnded = !isBattleOn(),
            hasFollowUpShot = grantsShot
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
        logFormationRecovery(troop, "before-skip")
        if (troop !in moraleExtraActionTroops) {
            troop.formation.current = if (canFullyRestoreFormation(troop)) {
                troop.formation.maximum
            } else RestoreFormationUseCase.execute(
                RestoreFormationUseCase.Input(
                    currentFormation = troop.formation.current,
                    maximumFormation = troop.formation.maximum
                )
            )
        }
        if (recoveryState(troop).phase == FormationRecoveryUseCase.Phase.READY) {
            formationRecovery[troop] = FormationRecoveryUseCase.State()
        }
        logFormationRecovery(troop, "after-skip")
        publishApplicationEvent(BattleEvent.TurnSkipped, onApplicationEvent)
        if (!isBattleOn()) publishApplicationEvent(
            BattleEvent.BattleEnded(isAttackerWinner()), onApplicationEvent
        )
        return BattleCommandResult(success = true, isMorale = false, battleEnded = !isBattleOn())
    }

    internal fun performSkipCommand(
        troop: Troop,
        onApplicationEvent: ((BattleEvent) -> Unit)? = null
    ): BattleCommandResult {
        // Preserve the game's random draw even though Skip does not grant an extra action.
        isMoraleTriggered(troop)
        pendingFollowUpShots.remove(troop)
        deferredMoveMorale.remove(troop)
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
                unit.formationDamageReductionPercent,
                isRanged = unit.isRanged
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
        var keepsDeferredMorale = false
        if (output.success) {
            pendingFollowUpShots.remove(troop)
            keepsDeferredMorale = deferredMoveMorale.remove(troop)
            publishApplicationEvent(
                BattleEvent.TroopShot(
                    troop.id, defender!!.id, remaining,
                    output.isLuck, output.isMorale, died
                ), onApplicationEvent
            )
        }
        if (!isBattleOn()) publishApplicationEvent(
            BattleEvent.BattleEnded(isAttackerWinner()), onApplicationEvent
        )
        return BattleCommandResult(
            success = output.success, rejection = output.rejection,
            isLuck = output.isLuck, isMorale = output.isMorale || keepsDeferredMorale,
            battleEnded = !isBattleOn()
        )
    }

    internal fun performShootCommand(
        troop: Troop, targetPosition: IBattleTile,
        onApplicationEvent: ((BattleEvent) -> Unit)? = null
    ): BattleCommandResult {
        // Preserve the existing morale draw for resolved commands, including rejections.
        val isMorale = isMoraleTriggered(troop)
        if (troop.isRanged && troop.currentAmount > 0 && getTroopTile(troop) != null &&
                hasAdjacentEnemy(troop) && getTroopOnTile(targetPosition) != null &&
                isTileOccupiedByEnemy(troop, targetPosition)
        ) {
            return BattleCommandResult(
                success = false, rejection = BattleRejection.SHOOTING_BLOCKED_BY_ENEMY,
                battleEnded = !isBattleOn()
            )
        }
        return performShootAction(troop, targetPosition, isMorale, onApplicationEvent)
    }

    private fun publishApplicationEvent(
        event: BattleEvent,
        onApplicationEvent: ((BattleEvent) -> Unit)? = null
    ) {
        fun recordAction(troopId: Int, isMorale: Boolean, interrupts: Boolean) {
            val troop = getTroopById(troopId) ?: return
            if (interrupts) formationRecovery[troop] =
                    FormationRecoveryUseCase.interrupt(recoveryState(troop))
            if (isMorale) moraleExtraActionTroops.add(troop)
            logFormationRecovery(troop, event.javaClass.simpleName)
        }

        fun recordIncomingAttack(troopId: Int) {
            val troop = getTroopById(troopId) ?: return
            formationRecovery[troop] = FormationRecoveryUseCase.interrupt(recoveryState(troop))
            logFormationRecovery(troop, "incoming-${event.javaClass.simpleName}")
        }
        when (event) {
            is BattleEvent.TroopMoved -> recordAction(event.troopId, event.isMorale, false)
            is BattleEvent.TroopAttacked -> {
                recordAction(event.attackerId, event.isMorale, true)
                recordIncomingAttack(event.defenderId)
            }

            is BattleEvent.TroopShot -> {
                recordAction(event.attackerId, event.isMorale, true)
                recordIncomingAttack(event.defenderId)
            }

            else -> Unit
        }
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
    private val formationRecovery = mutableMapOf<Troop, FormationRecoveryUseCase.State>()
    private val moraleExtraActionTroops = mutableSetOf<Troop>()
    private fun recoveryState(troop: Troop): FormationRecoveryUseCase.State =
            formationRecovery[troop] ?: FormationRecoveryUseCase.State()
    private fun hasDamagedFormation(troop: Troop): Boolean =
            troop.currentAmount > 0 && troop.hasFormation && troop.formation.maximum > 0 &&
                    troop.formation.current < troop.formation.maximum
    private fun hasAdjacentEnemy(troop: Troop): Boolean {
        val tile = getTroopTile(troop) ?: return true
        return getEnemies(troop).any { enemy ->
            enemy.currentAmount > 0 && getTroopTile(enemy)?.let { it in tile.neighbors } == true
        }
    }

    /** Presentation query only; preparation does not imply recovery in this activation. */
    fun hasFormationRecoveryChance(troop: Troop): Boolean =
            FormationRecoveryUseCase.hasChance(
                recoveryState(troop), hasDamagedFormation(troop), hasAdjacentEnemy(troop)
            )

    /** Both AI and Skip execution use the same full-recovery eligibility. */
    fun canFullyRestoreFormation(troop: Troop): Boolean =
            getCurrentTroop() === troop && troop !in moraleExtraActionTroops &&
                    FormationRecoveryUseCase.canRestore(
                        recoveryState(troop), hasDamagedFormation(troop), hasAdjacentEnemy(troop)
                    )
    private fun logFormationRecovery(troop: Troop, action: String) {
        if (!System.getProperty("battle.formation.verbose", "true").toBoolean()) return
        val state = recoveryState(troop)
        println(
            "[FormationRecovery] action=$action troop=${troop.unitName}#${troop.id} " +
                    "tile=${getTroopTile(troop)?.toPoint()} speed=${troop.speed} " +
                    "movementSpent=${state.movementSpent} phase=${state.phase} " +
                    "interrupted=${state.interrupted} adjacentEnemy=${hasAdjacentEnemy(troop)} " +
                    "formation=${troop.formation.current}/${troop.formation.maximum} " +
                    "configured=${troop.hasFormation} moraleExtra=${troop in moraleExtraActionTroops} " +
                    "chance=${hasFormationRecoveryChance(troop)} fullRecovery=${
                        canFullyRestoreFormation(
                            troop
                        )
                    }"
        )
    }
    private val pendingFollowUpShots = mutableSetOf<Troop>()
    private val moveAndShootGranted = mutableSetOf<Troop>()
    private val deferredMoveMorale = mutableSetOf<Troop>()

    /** The remaining action is a shot or Skip; movement and melee are unavailable. */
    fun hasPendingFollowUpShot(troop: Troop): Boolean = troop in pendingFollowUpShots
}
