package com.unciv.clean.adapters.music

import com.unciv.UncivGame
import com.unciv.clean.application.ports.MusicPort
import com.unciv.ui.audio.MusicMood
import com.unciv.ui.audio.MusicTrackChooserFlags

class MusicAdapter : MusicPort {
    override fun chooseTrack(civName: String, isWar: Boolean, setNextTurnFlag: Boolean) {
        val flags = if (setNextTurnFlag) MusicTrackChooserFlags.setNextTurn else MusicTrackChooserFlags.none
        UncivGame.Current.musicController.chooseTrack(
            civName,
            MusicMood.peaceOrWar(isWar),
            flags
        )
    }
}
