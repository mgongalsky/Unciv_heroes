# Arena armies with multiple squads

Encounters alternate between 4 and 5 requested squads: 4, 5, 4, 5, 4.Each side still uses its encounter 's single unit type. Total army counts and the player' s rounded - up 30 % bonus are unchanged.Distribution is deterministic and as even as possible: 26 fighters become 7 + 7 + 6 + 6 or 6 + 5 + 5 + 5 + 5.An army with fewer fighters than requested squads uses fewer squads, with no empty stacks . Retries create independent full - strength troops with the same distribution .

The arena list shows total fighters and individual squad sizes . Five -squad deployment uses rows 3, 1, 0, -1, -3 on each side of the production 14×8 field; previous deployment put the fifth squad outside the field.Deployment for up to four squads is unchanged .

Verification: build passed; ArenaArmyDistributionTest(3), ArenaRealBattleTest(3), and ArenaRunTest (9) passed . Real -field coverage includes 800 battles (20 matchups × 20 seeds × two squad limits), repeated outcomes with seed 42, unique occupied starting positions, complete turn queues, and fresh retry troops . The simulation limits are now 1000 actions and 100 actions without progress . These replace the single - squad test parameters described in arena -verification.md.The tests confirm battle completion, not an easy win rate.Splitting armies changes tactical balance even with unchanged total counts .

Launch with : desktop : runArena . Check movement and targeting with several squads, the fifth squad's visibility, and the squad breakdown in the arena list. UI verification scope: com.unciv.testing.UiGoldenTest#arena. Visual review and baseline approval remain outstanding; do not update baselines solely to remove a failure.
