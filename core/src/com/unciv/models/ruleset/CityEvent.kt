package com.unciv.models.ruleset

import com.unciv.logic.city.CityConstructions
import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.INonPerpetualConstruction
import com.unciv.logic.city.RejectionReasons
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueTarget
import com.unciv.models.stats.INamed
import com.unciv.models.stats.Stat
import com.unciv.ui.utils.extensions.toPercent
import kotlin.math.pow

class CityEvent : RulesetStatsObject(), INonPerpetualConstruction {
    override var name = ""
    override var requiredTech: String? = null
    override var cost: Int = 0
    override val hurryCostModifier: Int = 100 // Базовый множитель для расчета стоимости ускорения

    var description = ""
    var requiredBuilding: String = "" // Здание, активирующее событие
    var duration: Int = 0 // Длительность в ходах

    var resourceRequirementsInternal = HashMap<String, Int>()

    // Логика применения эффектов
    fun applyEffects(city: CityInfo) {
        // Реализация применения эффектов
    }

    // Логика снятия эффектов
    fun removeEffects(city: CityInfo) {
        // Реализация удаления эффектов
    }


    // Создание ссылки (оставлено пустым, так как в контексте CityEvent пока не требуется)
    override fun makeLink() = ""

    override fun getUniqueTarget() = UniqueTarget.CityEvent

    override fun shouldBeDisplayed(cityConstructions: CityConstructions): Boolean = true

    override fun getResourceRequirements(): HashMap<String, Int> = resourceRequirementsInternal

    override fun requiresResource(resource: String): Boolean = false


        // Проверка, может ли событие быть построено
    override fun isBuildable(cityConstructions: CityConstructions): Boolean {
        // Простая реализация: проверяем наличие требуемого здания
        //return true
        return cityConstructions.containsBuildingOrEquivalent(requiredBuilding)
    }

    // Получение стоимости производства для цивилизации
    override fun getProductionCost(civInfo: CivilizationInfo): Int {
        return cost
    }

    // Получение стоимости покупки за указанный ресурс
    override fun getStatBuyCost(cityInfo: CityInfo, stat: Stat): Int? {
        // Используем базовую реализацию из интерфейса
        return 5
        //return getBaseBuyCost(cityInfo, stat)
    }

    // Получение причин, почему строительство может быть запрещено
    override fun getRejectionReasons(cityConstructions: CityConstructions): RejectionReasons {
        val reasons = RejectionReasons()
        if (!isBuildable(cityConstructions)) {
            //reasons.addRejection("Requires $requiredBuilding")
        }
        return reasons
    }

    // Событие, вызываемое после завершения строительства
    override fun postBuildEvent(cityConstructions: CityConstructions, boughtWith: Stat?): Boolean {
        //val civInfo = cityConstructions.cityInfo.civInfo

        cityConstructions.addCityEvent(name)


        //applyEffects(city)
        return true // Успешное завершение события
    }

    // Проверка, может ли событие быть куплено за указанный ресурс
    override fun canBePurchasedWithStat(cityInfo: CityInfo?, stat: Stat): Boolean {
        // Простая проверка: событие не может быть куплено за "производство" или "счастье"
        return stat != Stat.Production && stat != Stat.Happiness
    }

    // Проверка, может ли событие быть куплено в принципе
    override fun isPurchasable(cityConstructions: CityConstructions): Boolean {
        return canBePurchasedWithAnyStat(cityConstructions.cityInfo)
    }

    // Проверка, может ли быть куплено за любой ресурс
    override fun canBePurchasedWithAnyStat(cityInfo: CityInfo): Boolean {
        return Stat.values().any { canBePurchasedWithStat(cityInfo, it) }
    }

    // Получение базовой стоимости покупки за золото
    override fun getBaseGoldCost(civInfo: CivilizationInfo): Double {
        return (30.0 * getProductionCost(civInfo)).pow(0.75) * hurryCostModifier.toPercent()
    }

    // Получение базовой стоимости покупки за указанный ресурс
    override fun getBaseBuyCost(cityInfo: CityInfo, stat: Stat): Int? {
        return super.getBaseBuyCost(cityInfo, stat)
    }

    // Расчет стоимости с учетом увеличения стоимости за ранее купленные события
    override fun getCostForConstructionsIncreasingInPrice(baseCost: Int, increaseCost: Int, previouslyBought: Int): Int {
        return super.getCostForConstructionsIncreasingInPrice(baseCost, increaseCost, previouslyBought)
    }
}
