---
name: ui - golden - tests
description: Run, diagnose, and update deterministic UI golden screenshot tests in Unciv_heroes.Use when changing Scene2D layout, popups, skins, fonts, sprites, tiles, battle presentation, UI Playground scenarios, or when investigating visual regressions .
---

# UI Golden Tests

Use this workflow for visual changes in `Unciv_heroes` that are covered by deterministic screenshot scenarios.The golden runner creates a hidden LWJGL3 OpenGL context at 1280×800, waits for the real scenario screen to finish loading, captures the framebuffer, and compares every pixel with a committed platform -specific PNG . It is visually headless, but the machine must still be able to create an OpenGL context .

## Verify existing baselines

Run:

`./gradlew desktop:goldenTest`

Expected successful output contains one green `PASS` line per scenario and ends with `BUILD SUCCESSFUL` .

Current scenarios :

-`battle-troops` — swordsman and crossbowman on the real grass hex field;
-`battle-result-attacker-victory` — the deterministic attacker - victory result popup.Do not treat compilation alone as a golden -test pass . The custom `desktop:goldenTest` task must render and compare the PNG files .

## Diagnose a failure

A failed comparison prints the number of changed pixels and writes artifacts under:

`tests/golden/results/<platform>/`

Inspect:

-`<scenario>-actual.png` — newly rendered output;
-`<scenario>-diff.png` — exact changed pixels shown in red;
-`tests/golden/baseline/<platform>/<scenario>.png` — committed expectation .

Determine whether the difference is:

1.an intended visual change;
2.an unintended layout, sprite, skin, font, viewport, or rendering regression;
3.a platform difference caused by font rasterization or the OpenGL driver;
4.an incomplete or unstable scenario.Never update a baseline merely to make the task green . Review the actual image and diff first .

## Update approved baselines

After confirming that every visual difference is intentional, run:

`./gradlew desktop:goldenUpdate`

This replaces the baselines for the current operating - system platform and removes stale actual / diff artifacts.Then immediately run:

`./gradlew desktop:goldenTest`

Require all scenarios to print `PASS` . Commit the approved PNG files under `tests/golden/baseline/<platform>/` together with the UI change that required them .

## Add a scenario

Add deterministic scenario setup to `UiGoldenTestRunner` . Prefer reusing the same public scene or popup factory used by UI Playground so manual preview and golden capture exercise identical production UI .

A scenario must:

-use fixed data with no time, random, save - game, network, or user -input dependency;
-open only after Unciv resources and the real test screen are ready;
-have a stable name suitable for a PNG filename;
-render within the fixed 1280×800 viewport;
-avoid animations unless the runner explicitly waits for a deterministic final state;
-cleanly finish within the runner watchdog timeout.After adding it, generate and inspect its baseline, rerun verification, and commit the approved platform PNG .

## MaxVibes limitation

        MaxVibes IDE -native `TESTS` checks accept test classes, methods, packages, or files; they do not accept a custom Gradle task name.Do not disguise `desktop:goldenTest` as a normal JUnit scope.When the custom task cannot be launched by the active plugin channel, ask the user to run the Gradle task and use its complete output plus generated PNG artifacts as evidence.

## Completion checklist

        -[] The affected UI has a deterministic golden scenario.
-[] `desktop:goldenTest` ran rather than only compiling.
-[] Every scenario printed `PASS`.
-[] Any failed actual / diff images were reviewed.
-[] Baselines were updated only for intentional changes .
-[] Approved platform - specific PNG files are committed.
-[] Generated files under `tests/golden/results/` remain uncommitted.
