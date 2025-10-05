package com.unciv.clean.application.usecases

import com.unciv.clean.application.ports.*
import com.unciv.clean.domain.events.DomainEvent
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.PlayerType

/**
 * Чистый сценарий "Следующий ход". Не знает про UI/UncivGame/LibGDX.
 * Все внешние вещи (время, музыка, сессия, лог, ИИ, варвары) приходят портами.
 */
class NextTurnUseCase(
    private val clock: Clock,
    private val music: MusicPort,
    private val ai: AiPort,
    private val barbarians: BarbariansPort,
    private val session: UserSessionPort,
    private val logger: LoggerPort,
) {

    /**
     * Выполняет следующий ход для переданного состояния игры.
     * Возвращает доменные события, собранные в процессе (тот же список доступен через game.domainEvents.drain()).
     */
    fun execute(game: GameInfo): List<DomainEvent> {
        // ----- начало: копия логики из GameInfo.nextTurn(), но с заменами -----

        val previousHumanPlayer = game.getCurrentPlayerCivilization()
        var thisPlayer: CivilizationInfo = previousHumanPlayer
        var currentPlayerIndex = game.civilizations.indexOf(thisPlayer)

        fun endTurn() {
            thisPlayer.endTurn()
            currentPlayerIndex = (currentPlayerIndex + 1) % game.civilizations.size
            if (currentPlayerIndex == 0) {
                game.turns++
                if (game.simulateUntilMaxDebugTurn())
                    logger.debug("Starting simulation of turn %s", game.turns)
            }
            thisPlayer = game.civilizations[currentPlayerIndex]
        }

        // важно для мультиплеера — не пропустить ход игрока
        if (thisPlayer.isPlayerCivilization()) endTurn()

        while (
            thisPlayer.playerType == PlayerType.AI
            || game.turns < game.simulateUntilTurnForDebug()
            || (game.turns < game.simulateMaxTurns && game.simulateUntilWin)
            || (game.gameParameters.isOnlineMultiplayer &&
               (thisPlayer.isDefeated() || (thisPlayer.isSpectator() && thisPlayer.playerId != session.currentUserId)))
        ) {
            thisPlayer.startTurn()

            if (!thisPlayer.isDefeated() || thisPlayer.isBarbarian()) {
                // ИИ-ход
                ai.playAiTurn(thisPlayer)

                // варвары ходят после своего хода
                if (thisPlayer.isBarbarian() && !game.gameParameters.noBarbarians)
                    barbarians.updateEncampments()

                // выходим из симуляции при победе
                if (thisPlayer.victoryManager.hasWon() && game.simulateUntilWin) {
                    game.simulateUntilWin = false
                    break
                }
            }
            endTurn()
        }

        // сброс режима "симулировать до хода N"
        game.resetDebugSimTurnIfReached()

        // обновление текущего игрока/тайминга
        game.currentTurnStartTime = clock.nowMillis()
        game.currentPlayer = thisPlayer.civName
        game.currentPlayerCiv = game.getCivilization(game.currentPlayer)

        thisPlayer.startTurn()
        if (game.currentPlayerCiv.isSpectator()) game.currentPlayerCiv.popupAlerts.clear()

        if (game.turns % 10 == 0) {
            music.chooseTrack(
                civName = game.currentPlayerCiv.civName,
                isWar = game.currentPlayerCiv.isAtWar(),
                setNextTurnFlag = true
            )
        }

        // counters on visitables
        game.visitableUpdater()

        // уведомления о близких врагах + доменные события
        game.notifyOfCloseEnemyUnits(thisPlayer)

        // важное событие смены хода — уже может быть эмитировано в GameInfo, но убедимся
        if (game.domainEvents.peek().none { it is DomainEvent.TurnAdvanced }) {
            game.domainEvents.emit(DomainEvent.TurnAdvanced(game.turns, game.currentPlayer))
        }

        // ----- конец «копии» -----

        // вернём снимок событий
        return game.domainEvents.peek()
    }
}

/* --------- ВСПОМОГАТЕЛЬНЫЕ-«ШИМЫ» ДЛЯ МИНИМАЛЬНОГО ДИФФА ---------
   Эти extension'ы прячут обращения к UncivGame.Current.* из GameInfo,
   чтобы тело use-case оставалось почти не изменённым. Их реализации ниже — тонкие.
*/
private fun GameInfo.simulateUntilTurnForDebug(): Int = com.unciv.UncivGame.Current.simulateUntilTurnForDebug
private fun GameInfo.simulateUntilMaxDebugTurn(): Boolean = com.unciv.UncivGame.Current.simulateUntilTurnForDebug != 0
private fun GameInfo.resetDebugSimTurnIfReached() {
    if (turns == com.unciv.UncivGame.Current.simulateUntilTurnForDebug)
        com.unciv.UncivGame.Current.simulateUntilTurnForDebug = 0
}

