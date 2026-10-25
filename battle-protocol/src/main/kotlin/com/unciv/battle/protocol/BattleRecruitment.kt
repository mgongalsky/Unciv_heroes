package com.unciv.battle.protocol

/** Server-owned recruitment prices; clients submit only unit names and counts. */
data class WireRecruitmentUnit(val unitName: String, val cost: Int)
data class WireRecruitmentCatalog(
    val budget: Int,
    val maxStacks: Int,
    val units: List<WireRecruitmentUnit>
)
data class WireArmyStack(val unitName: String, val count: Int)

/** Seat-specific setup data. The opponent's selection is not exposed before combat. */
data class WireArmyLobby(
    val catalog: WireRecruitmentCatalog,
    val ownArmy: List<WireArmyStack>
)

data class ReadyBattleArmyRequest(
    val protocolVersion: Int,
    val battleNumber: Int,
    val stacks: List<WireArmyStack>
) {
    fun validate() {
        BattleProtocol.requireVersion(protocolVersion)
        require(battleNumber > 0) { "Invalid battle number" }
        require(stacks.size in 1..7) { "Expected one to seven stacks" }
        require(stacks.all { it.unitName.isNotBlank() && it.unitName.length <= 128 && it.count > 0 }) {
            "Invalid army stack"
        }
    }
}
