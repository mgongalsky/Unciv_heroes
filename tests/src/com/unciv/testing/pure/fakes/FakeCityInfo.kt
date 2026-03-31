package com.unciv.testing.pure.fakes

import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.PopulationManager

class FakeCityInfo : CityInfo() {

    init {
        population = FakePopulationManager()
    }

    inner class FakePopulationManager : PopulationManager() {

    }
}
