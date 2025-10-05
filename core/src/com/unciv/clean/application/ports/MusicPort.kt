package com.unciv.clean.application.ports

interface MusicPort {
    fun chooseTrack(civName: String, isWar: Boolean, setNextTurnFlag: Boolean)
}
