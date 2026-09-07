# Arena tier 1 verification

        The menu retains the current ArenaRun in memory . Starting a new run updates the menu immediately through ArenaScreen.onRunChanged.ArenaScreen.recreate preserves both the run and this callback . Battle callbacks finish the run captured when that battle started .

This retention covers returning to the existing menu and screen recreation.It does not persist across application restarts.

## Automated checks

        -`com.unciv.testing.pure.domain.arena.ArenaRunTest`: generation, bonus rounding, progress, retries, stale results, completion and invalid input .
-`com.unciv.testing.ArenaRealBattleTest`: all 20 directed matchups with seeds 0–19 on the production 14×8 field, real movement and formation rules; repeated outcomes with seed 42; independent fresh troops for retries.
-`com.unciv.testing.pure.application.battle.BattleSimulationDeterminismTest`: existing seeded simulation regression .
-`com.unciv.testing.UiGoldenTest#arena`: arena - start, arena - retry and arena - complete screenshot verification.
-`com.unciv.testing.UiGoldenUpdateTest#arena`: create / update those three baselines only after reviewing their appearance; immediately rerun the verification scope.The real -field test uses the arena climate (0.3, 0.5, 0.6), player count rounded up after a 30 % bonus, luck probability 0.05, morale probability 0.1, 300 action limit and 30 actions without progress . Both sides use AIBattlePolicy.It prints one ARENA statistics line per matchup . Successful completion does not assert an easy player win rate; counts remain provisional estimates from the earlier simplified -field balance report.The three real - field tests passed.Golden verification currently fails; baseline review and verification remain outstanding . Earlier reported golden successes were invalid: UncivGame.dispose called System.exit(
    0
) before JUnit could receive the result.The golden runner now releases screen / audio resources without exiting the JVM and throws on verification failure .

## Manual acceptance sequence

1.Open Arena from the main menu . Start a battle, move and attack with melee troops, and shoot when using an Archer .
2.Win: close the battle result and confirm progress advances exactly once and the next encounter becomes available .
3.Lose or leave a battle: confirm earlier wins remain and retry uses the same unit types / counts with fresh troops .
4.Complete all five battles and select New arena run.Record the five new matchups; confirm progress is zero .
5.Return immediately to the menu, reopen Arena, and confirm the same new matchups and zero progress remain .
6.Win the first battle in this new run, return to the menu and reopen Arena . Confirm those same matchups and progress 1 / 5 remain .
7.Resize the arena window, return to the menu and reopen it . Repeat new - run creation after a resize to check callback retention.8.Review arena -start - actual.png, arena - retry - actual.png and arena - complete - actual.png under tests / golden / results / < platform >. Confirm text, counts, buttons and all rows are readable . Create approved baselines and rerun the arena golden verification .

Manual gameplay and visual acceptance have not yet been confirmed .
