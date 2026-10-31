package com.unciv.pure.domain.city

/**
 * Определяет смысл входа в город по готовым фактам, не меняя мир.
 * Проверку юнита выполняет NeutralCityEntry; бой и передачу владения — соответствующие адаптеры.
 */
object NeutralCityEntryRules {
    enum class Decision {
        /** Обычный город: эти правила к нему не применяются. */
        NOT_NEUTRAL,
        /** Вход не даёт этому юниту права получить нейтральный город. */
        INELIGIBLE_UNIT,
        /** Живого гарнизона нет: город можно получить фактическим входом в центр. */
        ACQUIRE,
        /** Сначала требуется тактическая победа над гарнизоном. */
        FIGHT_GARRISON
    }

    /** Число защитников учитывает только живые стеки; стоимость движения здесь не проверяется. */
    fun decide(
        ownership: CityOwnership?,
        eligibleHero: Boolean,
        livingDefenders: Int
    ): Decision {
        require(livingDefenders >= 0) { "Living defender count cannot be negative" }
        if (ownership != CityOwnership.Neutral) return Decision.NOT_NEUTRAL
        if (!eligibleHero) return Decision.INELIGIBLE_UNIT
        return if (livingDefenders == 0) Decision.ACQUIRE else Decision.FIGHT_GARRISON
    }
}
