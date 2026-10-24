package com.unciv.infrastructure.battle.network

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.unciv.battle.protocol.BattleProtocol
import com.unciv.infrastructure.battle.network.BattleReplyJson.bool
import com.unciv.infrastructure.battle.network.BattleReplyJson.int
import com.unciv.infrastructure.battle.network.BattleReplyJson.long
import com.unciv.infrastructure.battle.network.BattleReplyJson.optional
import com.unciv.infrastructure.battle.network.BattleReplyJson.required
import com.unciv.infrastructure.battle.network.BattleReplyJson.text
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.Point

enum class RemoteRoomPhase { WAITING, ACTIVE, FINISHED, CLOSED }

data class RemoteRoomState(
    val scenarioId: String,
    val side: BattleSide,
    val phase: RemoteRoomPhase,
    val opponentJoined: Boolean,
    val attackerReady: Boolean,
    val defenderReady: Boolean,
    val battle: BattleSessionSnapshot,
    val battleNumber: Int = 1,
    val attackerRematch: Boolean = false,
    val defenderRematch: Boolean = false,
    val report: com.unciv.pure.domain.battle.BattleReport? = null,
    val recruitment: com.unciv.pure.application.battle.recruitment.BattleArmySelection? = null,
    val ownArmy: List<com.unciv.pure.application.battle.recruitment.BattleArmyStack> = emptyList()
)

internal object BattleRoomJson {
    fun decode(text: String): RemoteRoomState {
        val root = JsonReader().parse(text)
        require(root.isObject)
        BattleProtocol.requireVersion(root.int("protocolVersion"))
        val session = root.required("battle")
        val state = session.required("state")
        fun side(value: JsonValue) = BattleSide.valueOf(value.text())
        val troops = state.required("troops").also { require(it.isArray) }.map { troop ->
            require(troop.isObject)
            BattleTroopSnapshot(
                id = troop.int("id"),
                side = side(troop.required("side")),
                slot = troop.int("slot"),
                unitName = troop.required("unitName").text(),
                initialAmount = troop.int("initialAmount"),
                currentAmount = troop.int("currentAmount"),
                currentHealth = troop.int("currentHealth"),
                maxHealth = troop.int("maxHealth"),
                speed = troop.int("speed"),
                damage = troop.int("damage"),
                rangedStrength = troop.int("rangedStrength"),
                formation = troop.int("formation"),
                maximumFormation = troop.int("maximumFormation"),
                position = troop.optional("position")?.let { Point(it.int("x"), it.int("y")) },
                retaliationBudget = troop.int("retaliationBudget"),
                normalRetaliationBudget = troop.int("normalRetaliationBudget"),
                hasPendingFollowUpShot = troop.bool("hasPendingFollowUpShot"),
                hasFormationRecoveryChance = troop.bool("hasFormationRecoveryChance"),
                supportBonusPercent = troop.int("supportBonusPercent"),
                flankingLevel = troop.int("flankingLevel"),
                formationHealthPercent = troop.int("formationHealthPercent"),
                formationDamageReductionPercent = troop.int("formationDamageReductionPercent")
            )
        }
        val queue = state.required("turnQueue").also { require(it.isArray) }.map {
            require(it.isLong && it.asLong() in 1L..Int.MAX_VALUE.toLong()); it.asInt()
        }
        val current = state.optional("currentTroopId")?.let {
            require(it.isLong && it.asLong() in 1L..Int.MAX_VALUE.toLong()); it.asInt()
        }
        val finished = state.bool("finished")
        val commands = state.required("availableCommands").also { require(it.isArray) }
            .map(BattleAvailableCommandJson::decode)
        require(commands.all { it.troopId == current })
        require(!finished || commands.isEmpty())
        val presentation =
                BattlePresentationJson.decode(requireNotNull(state.optional("presentation")) {
                    "Battle server does not provide presentation data. Update the server."
                })
        require(presentation.attacks.all { it.command in commands })
        require(presentation.movementRanges.all { range -> troops.any { it.id == range.troopId } })
        val report = root.optional("report")?.let { value ->
            require(value.isObject && finished)
            fun losses(name: String) = value.required(name).also { require(it.isArray) }.map {
                val id = it.int("troopId").also { id -> require(id > 0) }
                val amount = it.int("amount").also { count -> require(count > 0) }
                com.unciv.pure.domain.battle.BattleLoss(id, it.required("unitName").text(), amount)
            }
            com.unciv.pure.domain.battle.BattleReport(
                value.optional("winner")?.let(::side),
                losses("attackerLosses"), losses("defenderLosses")
            ).also {
                require(it.winner == state.optional("winner")?.let(::side))
            }
        }
        return RemoteRoomState(
            root.required("scenarioId").text(), side(root.required("side")),
            RemoteRoomPhase.valueOf(root.required("phase").text()), root.bool("opponentJoined"),
            root.bool("attackerReady"), root.bool("defenderReady"),
            BattleSessionSnapshot(
                session.long("revision"), session.long("lastSequence"),
                session.optional("lastReply")?.let(BattleReplyJson::decode),
                BattleStateSnapshot(
                    troops,
                    queue,
                    state.int("remainingTurnsInRound"),
                    current,
                    state.optional("currentSide")?.let(::side),
                    finished,
                    state.optional("winner")?.let(::side),
                    state.optional("field")?.let(::decodeField),
                    commands,
                    presentation
                )
            ),
            battleNumber = root.int("battleNumber").also { require(it > 0) },
            attackerRematch = root.bool("attackerRematch"),
            defenderRematch = root.bool("defenderRematch"), report = report
        )
    }

    private fun decodeField(value: JsonValue): BattleFieldDescription {
        require(value.isObject)
        val width = value.int("width")
        val height = value.int("height")
        require(width > 1 && height > 0 && width.toLong() * height <= 4096L)
        val nodes = value.required("cells")
        require(nodes.isArray && nodes.size.toLong() == width.toLong() * height)
        fun finiteNumber(node: JsonValue, name: String): Double {
            val number = node.required(name)
            require(number.isDouble || number.isLong)
            return number.asDouble().also { require(it.isFinite()) }
        }
        val cells = nodes.map { node ->
            require(node.isObject)
            val position = node.required("position")
            val point = Point(position.int("x"), position.int("y"))
            require(point.x in -BattleProtocol.MAX_COORDINATE..BattleProtocol.MAX_COORDINATE)
            require(point.y in -BattleProtocol.MAX_COORDINATE..BattleProtocol.MAX_COORDINATE)
            val base = node.required("baseTerrain").text().also { require(it.isNotBlank()) }
            val features = node.required("features").also { require(it.isArray) }.map {
                it.text().also { name -> require(name.isNotBlank()) }
            }
            BattleFieldCell(point, base, features, finiteNumber(node, "temperature"),
                finiteNumber(node, "humidity"), node.bool("impassable"))
        }
        require(cells.map { it.position }.distinct().size == cells.size)
        return BattleFieldDescription(width, height, cells)
    }
}
