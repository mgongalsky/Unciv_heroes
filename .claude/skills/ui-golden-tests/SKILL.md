---
name: ui-golden-tests
description: Run, diagnose, and update deterministic UI golden screenshot tests in Unciv_heroes. Use when changing Scene2D layout, popups, skins, fonts, sprites, tiles, battle presentation, UI Playground scenarios, or when investigating visual regressions.
---

# UI Golden Tests

Use this workflow for visual changes covered by deterministic screenshot scenarios. The runner
creates a hidden LWJGL3 OpenGL context at 1280×800, waits for the real scenario screen, captures the
framebuffer, and compares every pixel with a platform-specific baseline.

## Prefer the narrowest relevant scenario

Current scenario names and IDE scopes:

- `battle-troops` → `com.unciv.testing.UiGoldenTest#battleTroops`
- `battle-result-attacker-victory` → `com.unciv.testing.UiGoldenTest#battleResultAttackerVictory`
- `battle-tile-highlights` → `com.unciv.testing.UiGoldenTest#battleTileHighlights`
- `battle-turn-queue` → `com.unciv.testing.UiGoldenTest#battleTurnQueue`
- `battle-troop-info` → `com.unciv.testing.UiGoldenTest#battleTroopInfo`
- `battle-threat-preview` → `com.unciv.testing.UiGoldenTest#battleThreatPreview`
- `split-troop-empty` → `com.unciv.testing.UiGoldenTest#splitTroopEmpty`
- `split-troop-occupied` → `com.unciv.testing.UiGoldenTest#splitTroopOccupied`
- `army-troop-info` → `com.unciv.testing.UiGoldenTest#armyTroopInfo`

The split scenarios use the production dialog with 35 Archers and either an empty target or
12 target Archers. The army-info scenario covers the persistent statistics window with OK.
Their factories are shared with UI Playground. The Army interactions preview uses real slot
listeners for selection, Shift-splitting, double-left-click inspection and right-button hold.

The tile-highlights scenario covers both movement shading levels, normal and reinforced control
markers, pointer exit, appearance changes during hover, and removal of stale markers.

When a change affects one screen, run only its method. Run the entire class
`com.unciv.testing.UiGoldenTest` for cross-cutting changes to skins, fonts, atlases, shared popup
layout, rendering infrastructure, or before merging a broad UI change.

From Gradle, run all scenarios with:

`./gradlew desktop:goldenTest`

Run one scenario with:

`./gradlew desktop:goldenTest --args="--scenario=battle-troops"`

## Update approved baselines

Never update a baseline merely to make verification green. Review the actual and diff first.

IDE-native targeted update scopes use the same method name in `UiGoldenUpdateTest`:

- `com.unciv.testing.UiGoldenUpdateTest#battleTroops`
- `com.unciv.testing.UiGoldenUpdateTest#battleResultAttackerVictory`
- `com.unciv.testing.UiGoldenUpdateTest#battleTileHighlights`
- `com.unciv.testing.UiGoldenUpdateTest#battleTurnQueue`
- `com.unciv.testing.UiGoldenUpdateTest#battleTroopInfo`
- `com.unciv.testing.UiGoldenUpdateTest#battleThreatPreview`
- `com.unciv.testing.UiGoldenUpdateTest#splitTroopEmpty`
- `com.unciv.testing.UiGoldenUpdateTest#splitTroopOccupied`
- `com.unciv.testing.UiGoldenUpdateTest#armyTroopInfo`

Run the whole `UiGoldenUpdateTest` class only when every baseline change is intentional.

From Gradle, update all scenarios with:

`./gradlew desktop:goldenUpdate`

Update one scenario with:

`./gradlew desktop:goldenUpdate --args="--scenario=battle-troops"`

After every update, immediately run the matching verification method and require PASS.

## Diagnose a failure

Failure artifacts are written under `tests/golden/results/<platform>/`:

- `<scenario>-actual.png` — newly rendered output;
- `<scenario>-diff.png` — exact changed pixels in red;
- `tests/golden/baseline/<platform>/<scenario>.png` — committed expectation.

Classify the difference as an intended visual change, a regression, a platform-specific font/driver
difference, or an unstable scenario. Commit approved baseline PNGs with the UI change; never commit
generated results.

## Add a scenario

Add deterministic setup to `UiGoldenTestRunner`, preferably reusing the same public factory as UI
Playground. Give it a stable filename-safe name, fixed input data, no network/save/random
dependency, no uncontrolled animation, and a layout that fits 1280×800.

Add matching methods to both `UiGoldenTest` and `UiGoldenUpdateTest`. Document their method scopes
in this skill. Generate and inspect the new baseline, then verify it through the narrow method
scope.

## Completion checklist

- [ ] The affected UI has a deterministic scenario.
- [ ] The narrowest relevant scenario method was run.
- [ ] Broad shared changes ran the entire verification class.
- [ ] Every requested scenario printed PASS.
- [ ] Failed actual/diff images were reviewed.
- [ ] Baselines were updated only for intentional changes.
- [ ] Every updated scenario was verified immediately afterward.
- [ ] Approved platform PNGs are committed and generated results are not.
