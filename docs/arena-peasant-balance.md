# Peasant versus Archer arena balance

The original base of 20 Peasants against 5 Archers came from a simplified movement simulation. That
simulation has been replaced with ordinary BattleManager battles using real movement and
zone-of-control rules. BalanceUnitSources loads game data, including formation parameters.

The current base for this directed matchup is 36 Peasants against 5 Archers. Tier bonuses produce
47, 44, 40 and 36 Peasants at +30%, +20%, +10% and 0% respectively.

## Validation after unifying battle execution

ArenaRealBattleTest.peasantArcherTierWinRates was rerun on combat seeds 60–159, with a 14×8
battlefield and climate (0.3, 0.5, 0.6) for both sides. Configured luck probability is 0.05 and
morale probability is 0.1; actual effects also depend on army combat data. All 800 battles finished.

| Bonus | Peasants | Archers | Wins with 4 squads | Wins with 5 squads |
| --- | --- | --- | --- | --- |
| 30% | 47 | 5 | 100/100 | 100/100 |
| 20% | 44 | 5 | 100/100 | 100/100 |
| 10% | 40 | 5 | 100/100 | 100/100 |
| 0% | 36 | 5 | 84/100 | 77/100 |

Raw results: `tests/balance-results/arena-peasant-tier-validation.txt`.

Live AIBattle delegates to the same AIBattlePolicy used by simulations. Both execute BattleManager
commands and use BattleManager.completeAction for turn completion. Post-battle army recovery remains
a separate operation.

StandaloneBattleSetup creates pure armies and a tactical field without a world map, civilization,
application or graphics context. Its terrain rules are shared with the game's MapGenerator. The
parity test compares ordered neighbors, passability, events, action counts and final troop state in
20 paired Peasant/Archer battles. A separate test runs 40 battles with Gdx.app, Gdx.graphics and
Gdx.gl absent. BattleStateIsolationTest checks retaliation and effect-probability independence
between battles with identical troop IDs.

These results describe the shared AI policy on the tested field and seeds. They do not guarantee
human victories or establish balance for every terrain and army composition. Other arena matchup
counts remain provisional: the all-pairs test verifies completion, not equal win rates. Squad
splitting, rounding and target choices can make win rates non-monotonic.

Restart the application to use the corrected matchup table; existing in-memory runs retain their
encounters. UI golden baseline verification from earlier interface changes remains unresolved.
