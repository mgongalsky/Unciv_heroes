package com.unciv.di

import com.unciv.logic.GameInfo
import com.unciv.models.ruleset.Ruleset
import org.koin.dsl.module

val gameModule = module {
    single<Ruleset> { GameInfo.monsterRuleset }
}
