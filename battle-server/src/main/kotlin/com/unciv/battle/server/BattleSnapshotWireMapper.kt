package com.unciv.battle.server

import com.unciv.battle.protocol.*
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.session.BattleSessionSnapshot
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.Point

object BattleSnapshotWireMapper {
    private fun BattleSide.wire() = when (this) {
        BattleSide.ATTACKER -> WireSide.ATTACKER
        BattleSide.DEFENDER -> WireSide.DEFENDER
    }
    private fun Point.wire() = WirePoint(x, y)
    private fun command(value: BattleCommand): WireAvailableCommand = when (value) {
        is BattleCommand.Skip -> WireAvailableCommand(WireAction.SKIP, value.troopId)
        is BattleCommand.Move -> WireAvailableCommand(WireAction.MOVE, value.troopId, value.target.wire())
        is BattleCommand.Shoot -> WireAvailableCommand(WireAction.SHOOT, value.troopId, value.target.wire())
        is BattleCommand.Attack -> WireAvailableCommand(WireAction.ATTACK, value.troopId,
            value.target.wire(), value.attackFrom?.wire())
    }

    fun encode(snapshot: BattleSessionSnapshot): WireSessionSnapshot {
        val state = snapshot.state
        return WireSessionSnapshot(
            snapshot.revision, snapshot.lastSequence, snapshot.lastReply?.let(BattleReplyWireMapper::encode),
            WireBattleState(
                state.troops.map { troop ->
                    WireTroopSnapshot(
                        troop.id, troop.side.wire(), troop.slot, troop.unitName,
                        troop.initialAmount, troop.currentAmount, troop.currentHealth, troop.maxHealth,
                        troop.speed, troop.damage, troop.rangedStrength, troop.formation, troop.maximumFormation,
                        troop.position?.wire(), troop.retaliationBudget,
                        troop.normalRetaliationBudget, troop.hasPendingFollowUpShot,
                        troop.hasFormationRecoveryChance, troop.supportBonusPercent, troop.flankingLevel,
                        troop.formationHealthPercent, troop.formationDamageReductionPercent
                    )
                },
                state.turnQueue.toList(), state.remainingTurnsInRound, state.currentTroopId,
                state.currentSide?.wire(), state.finished, state.winner?.wire(),
                state.field?.let { field ->
                    WireFieldDescription(field.width, field.height, field.cells.map { cell ->
                        WireFieldCell(cell.position.wire(), cell.baseTerrain,
                            cell.features.toList(), cell.temperature, cell.humidity, cell.impassable)
                    })
                },
                state.availableCommands.map(::command),
                BattlePresentationWireMapper.encode(state.presentation, ::command)
            )
        )
    }

    fun encode(view: RoomView): WireRoomView = WireRoomView(
        BattleProtocol.VERSION, view.scenarioId, view.side.wire(),
        when {
            view.battle.state.finished -> WireRoomPhase.FINISHED
            view.attackerReady && view.defenderReady -> WireRoomPhase.ACTIVE
            else -> WireRoomPhase.WAITING
        },
        view.opponentJoined, view.attackerReady, view.defenderReady, encode(view.battle),
        view.battleNumber, view.attackerRematch, view.defenderRematch,
        view.report?.let { report ->
            fun losses(values: List<com.unciv.pure.domain.battle.BattleLoss>) = values.map {
                WireBattleLoss(it.troopId, it.unitName, it.amount)
            }
            WireBattleReport(report.winner?.wire(), losses(report.attackerLosses), losses(report.defenderLosses))
        },
        view.recruitment?.let { selection ->
            WireArmyLobby(
                WireRecruitmentCatalog(selection.budget, selection.maxStacks,
                    selection.units.map { WireRecruitmentUnit(it.unitName, it.cost) }),
                view.ownArmy.map { WireArmyStack(it.unitName, it.count) }
            )
        }
    )
}
