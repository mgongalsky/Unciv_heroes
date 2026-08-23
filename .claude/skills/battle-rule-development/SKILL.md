---
name: battle - rule - development
description: Develop, test, simulate, and evaluate a new tactical battle rule in Unciv_heroes.Use when adding or changing battle mechanics, unit interactions, targeting, movement, damage, morale, luck, terrain effects, AI behavior, balance parameters, or when comparing alternative battle -rule designs .
---

# Battle Rule Development

Use this workflow for every new or changed battle mechanic in `Unciv_heroes` .

The objective is not merely to implement a rule.The objective is to make the rule explicit, deterministic, testable, usable by both player and AI, measurable in headless simulations, and assessable through manual play.Read `BATTLE_SIMULATION_ROADMAP.md` before making architectural changes to the battle simulation infrastructure.

## 1.Clarify the gameplay hypothesis

        Before changing production code, state:

-the player -facing rule in one or two sentences;
-what tactical decision the rule is intended to create;
-which units, actions, tiles, phases, and edge cases it affects;
-whether the rule is deterministic or uses randomness;
-expected behavior for AI;
-one or more tunable parameters;
-what evidence would make the rule feel successful or unsuccessful .

If a missing choice would materially change the mechanic, ask the user.Otherwise make the smallest reversible assumption and state it.Do not begin by restructuring unrelated code.

## 2.Audit the existing path

        Trace the mechanic through only the relevant layers :

1.domain / value types;
2.pure application rule or use case;
3.`BattleCommand` execution and `BattleRejection`;
4.application `BattleEvent`;
5.`BattlePolicy` / `AIBattlePolicy` decision;
6.`BattleSimulationRunner` and batch metrics;
7.BattleScreen presentation or input mapping, if visible UI behavior changes;
8.existing unit, characterization, realistic - field, AI, and simulation tests.Search usages before removing, renaming, or changing a public or compatibility contract.Treat legacy request / result / event adapters as compatibility code.Do not route a new mechanic through them unless an existing compatibility test specifically requires it .

## 3.Define the rule as values

Prefer a small pure function or use case whose input contains values and explicit facts rather than BattleManager, Scene2D, Gdx, TileGroup, or mutable UI objects .

A good rule input may contain :

-troop ids and relevant stats;
-`Point` coordinates or calculated distance;
-explicit booleans such as targetIsEnemy, targetIsReachable, hasLineOfSight;
-explicit terrain or status modifiers;
-injected probability / random result;
-configuration values such as range, penalty, or duration .

A good output contains :

-success / failure;
-calculated value or state transition;
-a typed rejection reason when applicable;
-facts needed to publish an application event.Avoid hidden reads of global constants when the value is part of the experiment.Prefer an explicit parameter or configuration seam.

## 4.Write tests before wiring

        Start with focused JUnit 4 unit tests for the pure rule.Cover at minimum:

-normal success;
-exact boundary values;
-just inside and just outside the boundary;
-invalid target or impossible action;
-interaction with existing morale / luck / damage / movement rules when relevant;
-nullable or absent optional data;
-no mutation on rejection;
-deterministic behavior for a fixed random input .

Name tests in gameplay language.Assert value outputs and state changes, not implementation details.Do not use BattleScreen, Scene2D, Gdx, Koin, real maps, or full armies in a pure rule test unless the rule genuinely depends on them.

## 5.Wire through the command API

Connect the rule through `BattleCommand` execution .

Preserve these invariants:

-validate before mutating whenever possible;
-rejected commands do not mutate battlefield state;
-random values are consumed in a characterized order;
-successful mutation happens before its corresponding event is observed;
-lethal action event is emitted before `BattleEnded`;
-only application `BattleEvent` is primary in new code;
-player and AI use the same command executor;
-current - turn ownership is enforced by the orchestrator or command boundary .

Add or update a focused command -facade test for the integration seam.Do not make BattleScreen decide the rule.The screen may collect UI facts, map coordinates, submit commands, and render events.

## 6.Update AI as a policy

Change `AIBattlePolicy` when the mechanic affects decision - making.Keep selection separate from execution:

-policy returns `BattleCommand?`;
-policy does not call `BattleManager.execute`;
-policy does not mutate the battlefield;
-direct policy tests assert the selected command;
-`AIBattle` remains a thin execution adapter for UI - driven battles .

Specify what AI should do when the preferred action is unavailable . Examples include choosing another target, moving into range, defending, or skipping .

Preserve existing target priorities unless the mechanic intentionally changes them .

## 7.Verify with headless simulation

        Add or update at least one `BattleSimulationRunner` integration scenario when the mechanic can affect multi -turn behavior .

Check relevant outcomes:

-battle reaches victory;
-stalemate is detected;
-`maxTurns` prevents hangs;
-application events remain ordered;
-same scenario, policies, configuration, and seed produce the same normalized transcript;
-no Gdx, Scene2D, or BattleScreen dependency is introduced.

When comparing balance, use `BattleBatchSimulator` with a declared seed set.Record:

-attacker and defender win rates;
-average and median turns;
-stalemate and max - turn rates;
-other mechanic -specific metrics when useful .

Do not turn exploratory win - rate observations into brittle unit assertions . Statistical assertions require fixed seeds, adequate sample size, and a stable invariant .

## 8.Run proportionate regression

During development, run the smallest relevant test scope after each behavioral slice .

Before declaring the mechanic complete, run:

-BUILD;
-new pure rule tests;
-relevant command / use -case tests;
-relevant AI policy tests;
-runner / determinism tests;
-affected realistic -field tests;
-the focused battle regression suite.Use IDE -native `BUILD` and `TESTS` checks in MaxVibes.Never invoke build or test commands through a shell command field.If a characterization test fails, first determine whether it found an unintended behavior change or whether the old behavior is intentionally being replaced . Never update an expectation merely to make the suite green.

## 9.Perform a manual playtest

        For player -visible mechanics, ask for or perform a short manual smoke test after automated checks are green .

Use a concrete scenario that makes the new decision visible . Record :

-whether the rule was understandable from the UI;
-whether player feedback and cursor / action availability matched execution;
-whether AI reacted sensibly;
-whether the mechanic created the intended decision;
-any surprising or degenerate behavior.A mechanically correct rule may still be uninteresting . Treat playtest feedback as product evidence, not as a test failure.

## 10.Report the experiment

Finish with a compact report containing :

-rule implemented;
-assumptions and tunable parameters;
-affected production seams;
-tests and simulation scenarios added;
-BUILD / test totals;
-batch metrics, if collected;
-manual playtest result;
-known limitations;
-one recommended next experiment .

Separate required follow - up from optional cleanup .

## Guardrails

-Do not couple new rule logic to BattleScreen, Scene2D, Gdx, TileGroup, or cursor rendering.
-Do not add a second execution path for AI.
-Do not add unseeded randomness to a test or simulation .
-Do not allow a simulation to run without a turn limit.
-Do not silently change randomness consumption or event ordering.
-Do not use global mutable configuration for a parameter that will be varied in experiments when an explicit input is practical .
-Do not broaden a mechanic into unrelated legacy cleanup.
-Do not count test quantity as proof of balance; use targeted assertions, simulations, and playtests .

## Completion checklist

        A battle rule is complete when all applicable items are true:

-[] Gameplay hypothesis and edge cases are explicit .
-[] Tunable values have an intentional configuration seam .
-[] Core rule is value-oriented and independently tested .
-[] Command execution uses the rule and preserves mutation / event ordering .
-[] Rejections are typed and do not mutate state.
-[] AI policy selects an appropriate command without executing it .
-[] Headless simulation covers the important multi - turn interaction .
-[] Random behavior is reproducible by seed .
-[] Batch metrics were collected when balance is part of the question.
-[] Focused and regression tests are green.
-[] Player -visible behavior received a manual smoke test.
-[] Outcome, limitations, and next experiment are documented.
