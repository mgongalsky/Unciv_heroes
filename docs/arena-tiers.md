# Arena tiers

        This supersedes the five -battle progression described in the earlier arena verification documents.Each tier contains three encounters.After three victories, the Next tier button generates the next tier and resets its progress to 0 / 3.Defeat, draw and abandonment allow retry without changing the tier or encounter composition .

The player bonus decreases by ten percentage points per tier : + 30 %, +20 %, +10 %, then 0 % for all subsequent tiers.Counts round up.At base strength 20, totals are 26, 24, 22 and 20.Opponent counts are unchanged . Requested squad counts within each tier are 4, 5, 4, limited by available fighters .

Tier generation is deterministic from the run seed and tier number.Matchups are distinct within a tier; they may recur in later tiers . Advancing mutates the existing ArenaRun so returning to the menu preserves the tier and progress in memory . Application restart persistence is not implemented.Build and 15 pure tests passed : ArenaRunTest (12) and ArenaArmyDistributionTest(
    3
).Tests cover bonus progression and its zero floor, three - win completion, duplicate transition attempts, stale battle results, retry composition and deterministic generation.Earlier multi -squad battle simulations used the first -tier + 30 % bonus; they do not establish balance for later tiers .

Launch :desktop:runArena.Win three battles, select Next tier, and confirm Tier 2, +20 %, progress 0 / 3 and updated troop totals.When entered through the main menu, return to it and reopen Arena to check retained tier / progress.Screenshot verification scope: com.unciv.testing.UiGoldenTest#arena.Visual review and approved baselines remain outstanding.
