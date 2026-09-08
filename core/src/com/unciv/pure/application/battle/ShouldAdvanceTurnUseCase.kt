package com.unciv.pure.application.battle

object ShouldAdvanceTurnUseCase {
    fun execute(actionResult: BattleCommandResult?): Boolean =
        actionResult?.isMorale != true &&
                !(actionResult?.success == true && actionResult.hasFollowUpShot)
}
