package com.unciv.testing.pure

import com.unciv.pure.domain.troop.ITroopDefinitionSource
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import org.koin.dsl.module

val testModule = module {
    single<ITroopDefinitionSource> { RulesetTroopDefinitionSource(get()) }
}
