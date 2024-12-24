package com.unciv.ui.worldscreen.bottombar

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.UncivGame
import com.unciv.logic.automation.unit.BattleHelper
import com.unciv.logic.automation.unit.UnitAutomation
import com.unciv.logic.battle.Battle
import com.unciv.logic.battle.BattleDamage
import com.unciv.logic.battle.CityCombatant
import com.unciv.logic.battle.ICombatant
import com.unciv.logic.battle.MapUnitCombatant
import com.unciv.logic.map.TileInfo
import com.unciv.models.AttackableTile
import com.unciv.models.UncivSound
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.translations.tr
import com.unciv.ui.audio.SoundPlayer
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.Fonts
import com.unciv.ui.utils.UnitGroup
import com.unciv.ui.utils.extensions.addBorderAllowOpacity
import com.unciv.ui.utils.extensions.addSeparator
import com.unciv.ui.utils.extensions.disable
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import com.unciv.ui.worldscreen.WorldScreen
import com.unciv.ui.worldscreen.bottombar.BattleTableHelpers.flashWoundedCombatants
import com.unciv.ui.worldscreen.bottombar.BattleTableHelpers.getHealthBar
import kotlin.math.max

class BattleTable(val worldScreen: WorldScreen): Table() {

    init {
        isVisible = false
        skin = BaseScreen.skin
        background = BaseScreen.skinStrings.getUiBackground(
            "WorldScreen/BattleTable",
            tintColor = BaseScreen.skinStrings.skinConfig.baseColor.apply { a = 0.8f }
        )

        defaults().pad(5f)
        pad(5f)
        touchable = Touchable.enabled
    }

    private fun hide() {
        isVisible = false
        clear()
        pack()
    }

    fun update() {
        if (!worldScreen.canChangeState) return hide()

        val attacker = tryGetAttacker() ?: return hide()

        if (attacker is MapUnitCombatant && attacker.unit.baseUnit.isNuclearWeapon()) {
            val selectedTile = worldScreen.mapHolder.selectedTile
                ?: return hide() // no selected tile
            simulateNuke(attacker, selectedTile)
        } else if (attacker is MapUnitCombatant && attacker.unit.isPreparingAirSweep()) {
            val selectedTile = worldScreen.mapHolder.selectedTile
                ?: return hide() // no selected tile
            simulateAirsweep(attacker, selectedTile)
        } else {

            // Here is handling of normal battle (not nukes or air strike)
            val defender = tryGetDefender() ?: return hide()
            if (attacker is CityCombatant && defender is CityCombatant) return hide()
            simulateBattle(attacker, defender)
        }

        isVisible = true
        pack()
        addBorderAllowOpacity(1f, Color.WHITE)
    }

    private fun tryGetAttacker(): ICombatant? {
        val unitTable = worldScreen.bottomUnitTable
        return if (unitTable.selectedUnit != null
                && !unitTable.selectedUnit!!.isCivilian()
                && !unitTable.selectedUnit!!.hasUnique(UniqueType.CannotAttack))
                    MapUnitCombatant(unitTable.selectedUnit!!)
        else if (unitTable.selectedCity != null)
            CityCombatant(unitTable.selectedCity!!)
        else null // no attacker
    }

    private fun tryGetDefender(): ICombatant? {
        val selectedTile = worldScreen.mapHolder.selectedTile ?: return null // no selected tile
        return tryGetDefenderAtTile(selectedTile, false)
    }

    private fun tryGetDefenderAtTile(selectedTile: TileInfo, includeFriendly: Boolean): ICombatant? {
        val verbose_protectedTiles = true // Enable verbose logging for debugging

        val attackerCiv = worldScreen.viewingCiv
        var defender: ICombatant? = Battle.getMapCombatantOfTile(selectedTile)

        // Log the current state of the selected tile
        if (verbose_protectedTiles) {
            println(
                "Checking defender at tile: ${selectedTile.position}. " +
                        "IsProtected=${selectedTile.hasProtector()}"
            )
        }

        // If there is no direct defender, check if the tile is protected
        if (defender == null && selectedTile.hasEnemyProtector(attackerCiv)) {
            if (verbose_protectedTiles) {
                println("Tile is protected. Checking protectors...")
            }

            // Find the first enemy protector of the tile
            val firstEnemyProtector = selectedTile.getEnemyProtectors(attackerCiv).first()
            if (verbose_protectedTiles) {
                println(
                    "Found enemy protector: ${firstEnemyProtector.name} at ${firstEnemyProtector.currentTile.position}"
                )
            }
            defender = MapUnitCombatant(firstEnemyProtector)
        } else if (verbose_protectedTiles) {
            println("No enemy protectors found for this tile.")
        }


        // If no defender or the defender is friendly and `includeFriendly` is false, return null
        if (defender == null || (!includeFriendly && defender.getCivInfo() == attackerCiv)) {
            if (verbose_protectedTiles) {
                println("No valid defender found for tile: ${selectedTile.position}")
            }
            return null
        }

        // Check if the defender is visible to the attacker
        val canSeeDefender =
                if (UncivGame.Current.viewEntireMapForDebug) {
                    true
                } else {
                    when {
                        defender.isInvisible(attackerCiv) -> attackerCiv.viewableInvisibleUnitsTiles.contains(selectedTile)
                        defender.isCity() -> attackerCiv.hasExplored(selectedTile)
                        else -> attackerCiv.viewableTiles.contains(selectedTile)
                    }
                }

        if (!canSeeDefender) {
            if (verbose_protectedTiles) {
                println("Defender at tile ${selectedTile.position} is not visible.")
            }
            return null
        }

        if (verbose_protectedTiles) {
            println("Defender found and visible: ${defender.getName()} at tile: ${selectedTile.position}")
        }

        return defender
    }

    private fun getIcon(combatant:ICombatant) =
        if (combatant is MapUnitCombatant) UnitGroup(combatant.unit,25f)
        else ImageGetter.getNationIndicator(combatant.getCivInfo().nation, 25f)

    private val quarterScreen = worldScreen.stage.width / 4

    private fun getModifierTable(key: String, value: Int) = Table().apply {
        val description = if (key.startsWith("vs "))
            ("vs [" + key.drop(3) + "]").tr()
        else key.tr()
        val percentage = (if (value > 0) "+" else "") + value + "%"
        val upOrDownLabel = if (value > 0f) "⬆".toLabel(Color.GREEN)
        else "⬇".toLabel(Color.RED)

        add(upOrDownLabel)
        val modifierLabel = "$percentage $description".toLabel(fontSize = 14).apply { wrap = true }
        add(modifierLabel).width(quarterScreen - upOrDownLabel.minWidth)
    }

    /**
     * Simulates a battle between an attacker and a defender, displaying the results and possible actions.
     * This function calculates potential damage, displays modifiers, and provides a button to initiate the attack.
     *
     * @param attacker The attacking combatant.
     * @param defender The defending combatant.
     */
    private fun simulateBattle(attacker: ICombatant, defender: ICombatant) {
        // Clear any existing content in the battle table.
        clear()

        // Display the attacker’s name and icon.
        val attackerNameWrapper = Table()
        val attackerLabel = attacker.getName().toLabel()
        attackerNameWrapper.add(getIcon(attacker)).padRight(5f)
        attackerNameWrapper.add(attackerLabel)
        add(attackerNameWrapper)

        // Display the defender’s name and icon.
        val defenderNameWrapper = Table()
        val defenderLabel = Label(defender.getName().tr(), skin)
        defenderNameWrapper.add(getIcon(defender)).padRight(5f)
        defenderNameWrapper.add(defenderLabel)
        add(defenderNameWrapper).row()

        addSeparator().pad(0f)

        // Show the attack and defense strengths with respective icons.
        val attackIcon = if (attacker.isRanged()) Fonts.rangedStrength else Fonts.strength
        val defenceIcon =
                if (attacker.isRanged() && defender.isRanged() && !defender.isCity() && !(defender is MapUnitCombatant && defender.unit.isEmbarked()))
                    Fonts.rangedStrength
                else Fonts.strength // Use appropriate icons based on attacker and defender types.
        add(attacker.getAttackingStrength().toString() + attackIcon)
        if (defender is MapUnitCombatant) {
            var amount = 0
            defender.unit.army.getAllTroops().forEach { troop -> if (troop != null) amount += troop.amount }
            add(amount.toString() + defenceIcon).row()
        } else
            add(defender.getDefendingStrength(attacker.isRanged()).toString() + defenceIcon).row()

        // Retrieve and display attack and defense modifiers.
        val attackerModifiers =
                BattleDamage.getAttackModifiers(attacker, defender).map {
                    getModifierTable(it.key, it.value)
                }
        val defenderModifiers =
                if (defender is MapUnitCombatant)
                    BattleDamage.getDefenceModifiers(attacker, defender).map {
                        getModifierTable(it.key, it.value)
                    }
                else listOf()

        for (i in 0..max(attackerModifiers.size, defenderModifiers.size)) {
            if (i < attackerModifiers.size) add(attackerModifiers[i]) else add()
            if (i < defenderModifiers.size) add(defenderModifiers[i]) else add()
            row().pad(2f)
        }

        // Check if the attacker cannot gain more XP from Barbarians.
        val maxXPFromBarbarians = attacker.getCivInfo().gameInfo.ruleSet.modOptions.constants.maxXPfromBarbarians
        if (attacker is MapUnitCombatant && attacker.unit.promotions.totalXpProduced() >= maxXPFromBarbarians
                && defender.getCivInfo().isBarbarian()) {
            add("Cannot gain more XP from Barbarians".toLabel(fontSize = 16).apply { wrap = true }).width(quarterScreen)
            row()
        }

        // Calculate the damage to the defender and attacker.
        var damageToDefender = BattleDamage.calculateDamageToDefender(attacker, defender, true)
        var damageToAttacker = BattleDamage.calculateDamageToAttacker(attacker, defender, true)

        // Normalize damage if both would die in the combat.
        if (damageToAttacker > attacker.getHealth() && damageToDefender > defender.getHealth()) {
            if (damageToDefender * attacker.getHealth() > damageToAttacker * defender.getHealth()) { // Defender dies first.
                damageToDefender = defender.getHealth()
                damageToAttacker *= (defender.getHealth() / damageToDefender.toFloat()).toInt()
            } else { // Attacker dies first.
                damageToAttacker = attacker.getHealth()
                damageToDefender *= (attacker.getHealth() / damageToAttacker.toFloat()).toInt()
            }
        } else if (damageToAttacker > attacker.getHealth()) damageToAttacker = attacker.getHealth()
        else if (damageToDefender > defender.getHealth()) damageToDefender = defender.getHealth()

        // Show the outcome of the combat for melee units attacking civilians or defeated cities.
        if (attacker.isMelee() &&
                (defender.isCivilian() || defender is CityCombatant && defender.isDefeated())) {
            add()
            val defeatedText = when {
                !defender.isCivilian() -> "Occupied!"
                (defender as MapUnitCombatant).unit.hasUnique(UniqueType.Uncapturable) -> ""
                else -> "Captured!"
            }
            add(defeatedText.toLabel())
        } else {
            add(getHealthBar(attacker.getHealth(), attacker.getMaxHealth(), damageToAttacker))
            add(getHealthBar(defender.getHealth(), defender.getMaxHealth(), damageToDefender))
        }

        row().pad(5f)
        val attackText: String = when (attacker) {
            is CityCombatant -> "Bombard"
            else -> "Attack"
        }
        val attackButton = attackText.toTextButton().apply { color = Color.RED }

        // Determine if the tile is attackable and prepare the attack button.
        var attackableTile: AttackableTile? = null

        if (attacker.canAttack()) {
            if (attacker is MapUnitCombatant) {
                attackableTile = BattleHelper
                    .getAttackableEnemies(attacker.unit, attacker.unit.movement.getDistanceToTiles())
                    .firstOrNull { it.tileToAttack == defender.getTile() }
            } else if (attacker is CityCombatant) {
                val canBombard = UnitAutomation.getBombardTargets(attacker.city).contains(defender.getTile())
                if (canBombard) {
                    attackableTile = AttackableTile(attacker.getTile(), defender.getTile(), 0f)
                }
            }
        }

        // Disable the attack button if attacking is not possible.
        /*
        if (!worldScreen.isPlayersTurn || attackableTile == null) {
            attackButton.disable()
            attackButton.label.color = Color.GRAY
        } else {
            attackButton.onClick(UncivSound.Silent) { // Sound will be played in onAttackButtonClicked.
                onAttackButtonClicked(attacker, defender, attackableTile, damageToAttacker, damageToDefender)
            }
        }

         */
        val verbose_protectedTiles = true
        // Prepare the attack button with debug logging.
        if (attackableTile != null) {
            if (verbose_protectedTiles) {
                println("Attack button enabled for attacker: ${attacker.getName()}, defender: ${defender.getName()}, " +
                        "tile to attack: ${attackableTile.tileToAttack.position}, tile to attack from: ${attackableTile.tileToAttackFrom.position}")
            }
            attackButton.onClick(UncivSound.Silent) { // Sound will be played in onAttackButtonClicked.
                onAttackButtonClicked(
                    attacker,
                    defender,
                    attackableTile,
                    damageToAttacker,
                    damageToDefender
                )
            }
        } else {
            if (verbose_protectedTiles) {
                println("Attack button disabled. Attacker: ${attacker.getName()}, defender: ${defender.getName()}. " +
                        "Reason: attackableTile is null.")
            }
            attackButton.disable()
            attackButton.label.color = Color.GRAY
        }
        // Add the attack button to the table.
        add(attackButton).colspan(2)

        // Finalize and position the battle table.
        pack()
        setPosition(worldScreen.stage.width / 2 - width / 2, 5f)
    }


    private fun onAttackButtonClicked(
        attacker: ICombatant,
        defender: ICombatant,
        attackableTile: AttackableTile,
        damageToAttacker: Int,
        damageToDefender: Int
    ) {
        val canStillAttack = Battle.movePreparingAttack(attacker, attackableTile)
        worldScreen.mapHolder.removeUnitActionOverlay() // the overlay was one of attacking
        // There was a direct worldScreen.update() call here, removing its 'private' but not the comment justifying the modifier.
        // My tests (desktop only) show the red-flash animations look just fine without.
        worldScreen.shouldUpdate = true
        //Gdx.graphics.requestRendering()  // Use this if immediate rendering is required

        if (!canStillAttack) return
        SoundPlayer.play(attacker.getAttackSound())
        Battle.attackOrNuke(attacker, attackableTile)

        worldScreen.flashWoundedCombatants(attacker, damageToAttacker, defender, damageToDefender)
    }


    private fun simulateNuke(attacker: MapUnitCombatant, targetTile: TileInfo){
        clear()

        val attackerNameWrapper = Table()
        val attackerLabel = attacker.getName().toLabel()
        attackerNameWrapper.add(getIcon(attacker)).padRight(5f)
        attackerNameWrapper.add(attackerLabel)
        add(attackerNameWrapper)

        val canNuke = Battle.mayUseNuke(attacker, targetTile)

        val blastRadius =
            if (!attacker.unit.hasUnique(UniqueType.BlastRadius)) 2
            else attacker.unit.getMatchingUniques(UniqueType.BlastRadius).first().params[0].toInt()

        val defenderNameWrapper = Table()
        for (tile in targetTile.getTilesInDistance(blastRadius)) {
            val defender = tryGetDefenderAtTile(tile, true) ?: continue

            val defenderLabel = Label(defender.getName().tr(), skin)
            defenderNameWrapper.add(getIcon(defender)).padRight(5f)
            defenderNameWrapper.add(defenderLabel).row()
        }
        add(defenderNameWrapper).row()

        addSeparator().pad(0f)
        row().pad(5f)

        val attackButton = "NUKE".toTextButton().apply { color = Color.RED }

        val canReach = attacker.unit.currentTile.getTilesInDistance(attacker.unit.getRange()).contains(targetTile)

        if (!worldScreen.isPlayersTurn || !attacker.canAttack() || !canReach || !canNuke) {
            attackButton.disable()
            attackButton.label.color = Color.GRAY
        }
        else {
            attackButton.onClick(attacker.getAttackSound()) {
                Battle.NUKE(attacker, targetTile)
                worldScreen.mapHolder.removeUnitActionOverlay() // the overlay was one of attacking
                worldScreen.shouldUpdate = true
            }
        }

        add(attackButton).colspan(2)

        pack()

        setPosition(worldScreen.stage.width / 2 - width / 2, 5f)
    }

    private fun simulateAirsweep(attacker: MapUnitCombatant, targetTile: TileInfo)
    {
        clear()

        val attackerNameWrapper = Table()
        val attackerLabel = attacker.getName().toLabel()
        attackerNameWrapper.add(getIcon(attacker)).padRight(5f)
        attackerNameWrapper.add(attackerLabel)
        add(attackerNameWrapper)

        val canAttack = attacker.canAttack()

        val defenderLabel = Label("???", skin)
        add(defenderLabel).row()

        addSeparator().pad(0f)

        val attackIcon = Fonts.rangedStrength
        add(attacker.getAttackingStrength().toString() + attackIcon)
        add("???$attackIcon").row()

        val attackerModifiers =
                BattleDamage.getAirSweepAttackModifiers(attacker).map {
                    getModifierTable(it.key, it.value)
                }

        for (modifier in attackerModifiers) {
            add(modifier)
            add()
            row().pad(2f)
        }

        add(getHealthBar(attacker.getHealth(), attacker.getMaxHealth(), 0))
        add(getHealthBar(attacker.getMaxHealth(), attacker.getMaxHealth(), 0))
        row().pad(5f)

        val attackButton = "Air Sweep".toTextButton().apply { color = Color.RED }

        val canReach = attacker.unit.currentTile.getTilesInDistance(attacker.unit.getRange()).contains(targetTile)

        if (!worldScreen.isPlayersTurn || !attacker.canAttack() || !canReach || !canAttack) {
            attackButton.disable()
            attackButton.label.color = Color.GRAY
        }
        else {
            attackButton.onClick(attacker.getAttackSound()) {
                Battle.airSweep(attacker, targetTile)
                worldScreen.mapHolder.removeUnitActionOverlay() // the overlay was one of attacking
                worldScreen.shouldUpdate = true
            }
        }

        add(attackButton).colspan(2)

        pack()

        setPosition(worldScreen.stage.width / 2 - width / 2, 5f)
    }
}
