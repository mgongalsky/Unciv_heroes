package com.unciv.pure.domain.victory

/** Цель карты со стабильным ID города и явным набором слотов, которым она доступна. */
data class CaptureCityObjective(
    var cityId: String = "",
    var eligibleSlotIds: ArrayList<String> = arrayListOf()
) {
    fun deepCopy(): CaptureCityObjective = copy(eligibleSlotIds = ArrayList(eligibleSlotIds))

    companion object {
        const val VICTORY_TYPE = "CaptureCity"

        /** Все цели участника должны существовать и принадлежать ему; пустой набор не даёт победу. */
        fun isCompleted(
            objectives: List<CaptureCityObjective>,
            slotId: String,
            playerId: String,
            cityOwners: Map<String, String?>
        ): Boolean {
            if (slotId.isBlank() || playerId.isBlank()) return false
            val assigned = objectives.filter { slotId in it.eligibleSlotIds }
            return assigned.isNotEmpty() && assigned.all {
                it.cityId.isNotBlank() && cityOwners[it.cityId] == playerId
            }
        }
    }
}
