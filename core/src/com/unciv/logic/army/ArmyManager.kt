package com.unciv.logic.army



class ArmyManager(
    private val army1: ArmyInfo, // Первая армия
    private val army2: ArmyInfo? = null // Вторая армия для обмена (опционально)
) {
    /** Добавляет отряд в указанную армию */
    fun addTroop(troop: TroopInfo, toArmy1: Boolean = true): Boolean {
        val targetArmy = if (toArmy1) army1 else army2 ?: return false
        return targetArmy.addTroop(troop)
    }

    /** Удаляет отряд из указанной армии */
    fun removeTroop(index: Int, fromArmy1: Boolean = true): TroopInfo? {
        val targetArmy = if (fromArmy1) army1 else army2 ?: return null
        return targetArmy.removeTroopAt(index)
    }

    /** Перемещает отряд между армиями */
    fun transferTroop(index: Int, toArmy1: Boolean): Boolean {
        if (army2 == null) return false // Если нет второй армии, перенос невозможен

        val fromArmy = if (toArmy1) army2 else army1
        val toArmy = if (toArmy1) army1 else army2

        val troop = fromArmy.removeTroopAt(index) ?: return false
        return toArmy.addTroop(troop)
    }

    /** Возвращает отряд по индексу из указанной армии */
    fun getTroop(index: Int, fromArmy1: Boolean = true): TroopInfo? {
        val targetArmy = if (fromArmy1) army1 else army2 ?: return null
        return targetArmy.getTroopAt(index)
    }


}
