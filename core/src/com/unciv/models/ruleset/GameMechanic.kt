package com.unciv.models.ruleset

import com.unciv.models.stats.INamed
import com.unciv.ui.civilopedia.FormattedLine
import com.unciv.ui.civilopedia.ICivilopediaText

/**
 * Represents a game mechanic entry for the Civilopedia
 * Contains explanations of core game mechanics like food consumption, army management, etc.
 */
class GameMechanic : INamed, ICivilopediaText {
    override lateinit var name: String
    
    /** List of text blocks that explain this game mechanic */
    override var civilopediaText: List<FormattedLine> = emptyList()

    override fun makeLink() = "GameMechanics/$name"
}
