package com.unciv.battle.server

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.battle.protocol.*
import com.unciv.infrastructure.battle.network.BattleHttpTransport
import com.unciv.infrastructure.battle.network.RemoteBattleLobby
import com.unciv.pure.application.battle.*
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BattlePreviewRoundTripTest {
    @Test
    fun preservesMovementControlMeleeRetaliationAndIndirectShot() = runBlocking {
        val origin = Point(0, 0)
        val target = Point(1, 0)
        val attack = BattleCommand.Attack(1, target, origin)
        val shoot = BattleCommand.Shoot(1, target)
        val hit = BattleAttackPreview.Hit(
            PreparedBattleDamage(10, 8, 1, 80, 15, 92, 25, 69),
            FlankingCalculator.Result(3, 2, 10, 20, 30),
            40, 50, 0, DamageResult(49, 2, 8, 11, false)
        )
        val melee = BattleAttackPreview(hit,
            hit.copy(preparation = null, remainingFormation = 7),
            retaliationBudget = 55, remainingRetaliationBudget = 12, usesStoredRetaliation = true)
        val ranged = BattleAttackPreview(hit, isRanged = true,
            rangedShot = RangedShotRules.Result(RangedShotRules.ShotType.INDIRECT,
                null, 8, 4, true, 50))
        val presentation = BattlePresentationSnapshot(
            tiles = ZoneOfControlTransition.Strength.values().mapIndexed { index, strength ->
                BattleTilePresentation(Point(index, 0), BattleTileAppearanceUseCase.Appearance(
                    listOf(1f, 0.5f, 0.7f)[index], strength))
            },
            movementRanges = listOf(BattleTroopMovementPresentation(1,
                BattleMovementPreviewUseCase.TileStyle.values().mapIndexed { index, style ->
                    BattleMovementTilePresentation(Point(index, 0), style)
                })),
            attacks = listOf(BattleAttackPresentation(attack, melee), BattleAttackPresentation(shoot, ranged))
        )
        val troop = BattleTroopSnapshot(1, BattleSide.ATTACKER, 0, "Archer", 10, 10,
            20, 20, 5, 8, 10, 40, 40, origin, 55, 60, false, true, 15, 2, 20, 50)
        val state = BattleStateSnapshot(listOf(troop), listOf(1), 1, 1, BattleSide.ATTACKER,
            false, null, availableCommands = listOf(attack, shoot), presentation = presentation)
        val snapshot = BattleSessionSnapshot(4, 0, null, state)
        fun json(value: Any) = Json().apply {
            setTypeName(null)
            setOutputType(JsonWriter.OutputType.json)
        }.toJson(value)
        val room = json(BattleSnapshotWireMapper.encode(RoomView(
            "scenario", BattleSide.ATTACKER, true, true, true, snapshot)))
        val transport = object : BattleHttpTransport {
            override suspend fun request(method: String, path: String, body: String?, token: String?): String =
                if (method == "POST" && path == "/v1/rooms")
                    json(BattleRoomTicket(BattleProtocol.VERSION, "abcdefgh", "a".repeat(43), WireSide.ATTACKER))
                else {
                    assertEquals("GET", method)
                    assertEquals("/v1/rooms/abcdefgh", path)
                    room
                }
        }
        assertEquals(snapshot, RemoteBattleLobby(transport).create("scenario").read())
    }
}
