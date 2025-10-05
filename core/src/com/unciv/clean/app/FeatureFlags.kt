package com.unciv.clean.app

data class FeatureFlags(
    val notificationsViaBus: Boolean = false,
    val writeVersionedSaves: Boolean = false
)
