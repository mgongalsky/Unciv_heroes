package com.unciv.clean.application.ports.persistence

enum class SaveWritePolicy {
    LEGACY_BY_DEFAULT,  // писать ровно как раньше (поведение=0)
    VERSIONED_V1        // писать конверт V1
}
