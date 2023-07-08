package com.unciv.logic.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.unciv.UncivGame
import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.event.hero.Troop
import com.unciv.models.stats.Stat
import com.unciv.ui.popup.Popup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode

enum class Visitability { none, once_per_hero, once_per_civ, regular, once, next_battle, takeable }

class Visitable() :
    IsPartOfGameInfoSerialization {
   /*
    companion object{
        /** Dummy object for serialization purposes only. We need a constructor() */
        val dummyTile = TileInfo()
    }


    */

    @Transient
    var improvement: String = ""

    @Transient
    lateinit var parentTile: TileInfo
    var visitedHeroesIDs = mutableSetOf<Int>()
    var visitedCivsIDs = mutableSetOf<Int>()
    var turnsToRefresh: Int = 0

    // We don't need to save it, because it is determined by [improvement] String
    @Transient
    var visitability: Visitability = Visitability.none


   // constructor()

    constructor(improvement0: String, tile0: TileInfo): this(){
        parentTile = tile0
        improvement = improvement0
    }

    init {
        //if (improvement.isEmpty())
        //    throw IllegalStateException("Improvement string cannot be empty. Specify it in the constructor and check manually, that it so not empty.")
        setVisitability()

    }

    /** Sets proper visitability. Make sure [improvement] string is correctly set. */
    fun setVisitability() {
        when (improvement) {
            "Citadel", "Manufactory" ->
                visitability = Visitability.once_per_hero
            "Trading post", "Holy site" ->
                visitability = Visitability.regular
            "Gold" ->
                visitability = Visitability.takeable
            else ->
                visitability = Visitability.none
        }

    }

    fun clone(): Visitable {
        // Carefull here, there can be infinite loop TileInfo <-> Visitable
        val toReturn = Visitable()
        toReturn.visitedHeroesIDs = visitedHeroesIDs
        toReturn.visitedCivsIDs = visitedCivsIDs
        //visitedHeroesIDs.forEach {it -> toReturn.visitedHeroesIDs.add(it)}
        //toReturn.visitedHeroesIDs = HashSet<Int>(visitedHeroesIDs)
        //toReturn.visitedCivsIDs = HashSet<Int>(visitedCivsIDs)
        toReturn.turnsToRefresh = turnsToRefresh
        //toReturn.improvement = improvement
        return toReturn
    }

    // TODO: Here we need to change hand-written messages into JSON-fields
    /** Action when [unit] visits this visitable. Returns true, if there is an effect of visit. */
    fun visit(unit: MapUnit): Boolean {
        when (visitability) {
            Visitability.takeable -> {
                unit.civInfo.addGold(5)
                parentTile.removeImprovement()
                return true
            }
            Visitability.once_per_hero -> {
                if (!visitedHeroesIDs.contains(unit.id)) { // This hero visits for the first time
                    when (improvement) {
                        "Citadel" -> {
                            unit.heroAttackSkill += 1
                            openVisitingPopup("You have just visited School of War.\n Your attack skill improved.\n Attack skill +1")
                            //parentTile.removeImprovement()
                            //parentTile.improvement = null
                        }
                        "Manufactory" -> {
                            unit.heroDefenseSkill += 1
                            openVisitingPopup("You see a forge. The smith gives your a new armor.\n Your defense skill improved.\n Defense skill +1")
                        }
                    }
                    visitedHeroesIDs.add(unit.id)
                    return true
                } else { // Already visited by this hero
                    when (improvement) {
                        "Citadel" ->
                            openVisitingPopup("Sorry, you have already visited School of War.\n The training program is for one time only.")
                        "Manufactory" ->
                            openVisitingPopup("Sorry, you have already visited this forge.\n The smith could not give you a better armor.")
                    }
                    return false
                }
            }
            Visitability.regular -> {
                if (turnsToRefresh == 0) { // Place is ready to visit a hero
                    when (improvement) {
                        "Trading post" -> {
                            unit.civInfo.addGold(5)
                            //unit.civInfo.addStat(Stat.Science, 10)
                            //Stat.
                            turnsToRefresh = 3
                            openVisitingPopup("You visit marketplace. You see a familiar merchant, whom you helped earlier.\n The merchant gives you some money.\n Gold +5")
                        }
                        "Holy site" -> {
                            // TODO: This does not work, should be improved
                            unit.addExtraMovementPoints(3f)
                            turnsToRefresh = 1
                            openVisitingPopup("You enter into the temple. You fill so good and motivated.\n You talk to the priest and ready for further crusade.\n Movement +3")
                        }
                    }
                    return true
                } else { // Place is not ready to host a hero, because recently was visited
                    when (improvement) {
                        "Trading post" ->
                            openVisitingPopup("Sorry, you have already visited \n this market no longer than 3 days ago.\n The merchant does not have money to give you yet.")
                        "Holy site" ->
                            openVisitingPopup("Sorry, you have already visited the temple today. \n Use your extra movement points and come back.")
                    }
                    return false
                }
            }
            else -> { // Other cases of visitability including non-visitables
                return true
            }

        }
    }
}

fun openVisitingPopup(text: String) {
    if (UncivGame.Current.worldScreen == null) return
    // Probably we need to check if computer visits that place also
    // if(UncivGame.Current.gameInfo?.currentPlayerCiv != UncivGame.Current.is) return
    val visitingPopup = Popup(UncivGame.Current.worldScreen!!)
    val stage_width = UncivGame.Current.worldScreen!!.stage.width
    val stage_height = UncivGame.Current.worldScreen!!.stage.height

    val textButtonStyle = TextButton.TextButtonStyle().apply {
        up = BaseScreen.skin.getDrawable("UI_button_up") // Set the up image using the skin
        down =
                BaseScreen.skin.getDrawable("UI_button_down") // Set the down image using the skin
        font = BaseScreen.skin.getFont("oldLondon") // Set the font using the skin
        fontColor = Color.WHITE // Set the font color

        // Adjust the padding around the text
        val padding = 10f // Adjust the padding value as needed
        up?.topHeight = padding
        up?.bottomHeight = padding
        up?.leftWidth = padding
        up?.rightWidth = padding
        down?.topHeight = padding
        down?.bottomHeight = padding
        down?.leftWidth = padding
        down?.rightWidth = padding
    }

// Set the font size programmatically
    textButtonStyle.font.data.setScale(0.3f) // Adjust the scale factor as needed (0.8f is an example)
    //textButtonStyle.font.data.

    //  visitingPopup.padTop(30f)
    visitingPopup.addGoodSizedLabel(text).padTop(30f)
    visitingPopup.row()
    //visitingPopup.addCloseButton("OK", KeyCharAndCode.BACK, BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)).center().maxWidth(stage_width / 20).maxHeight(stage_height/20)
    visitingPopup.addCloseButton("OK", KeyCharAndCode.BACK, textButtonStyle)
        .maxWidth(stage_width / 15).maxHeight(stage_height / 19)
    visitingPopup.open()
}


