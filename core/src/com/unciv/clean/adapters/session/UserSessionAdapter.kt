package com.unciv.clean.adapters.session

import com.unciv.UncivGame
import com.unciv.clean.application.ports.UserSessionPort

class UserSessionAdapter : UserSessionPort {
    override val currentUserId: String
        get() = UncivGame.Current.settings.multiplayer.userId
}
