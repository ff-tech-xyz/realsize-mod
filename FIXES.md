# RealSize v2 fixes and acceptance

Status: open implementation work. README and ARCHITECTURE describe the final
product in present tense at Elijah's request; this tracker describes reality.
The supplied audit concerns the removed legacy implementation. Its exact mob
visibility distances, allocation rates, and gameplay outcomes are reported
findings, not locally reproduced measurements. Do not mark them verified from
this document alone.

Current checkout: executable scaffold with isolated main/client entrypoints,
Fabric metadata, a packaged JAR, an artifact smoke test, and a real server
GameTest. It uses one 26.3 project, Java 25, non-remapping Loom, and no identity
mapping JAR. No gameplay features are implemented. The former FEATURES.md mentioned
in the audit is not present in this checkout; ARCHITECTURE is the active contract.

## Open fixes

- [ ] **RS-01 — Tiny-mob visibility.** Drop server tracking extensions; add the
  optional client unscaled-distance correction in the same JAR. Reproduce the
  bee disappearance around 12 blocks before fixing it. Compare vanilla and
  modded clients, toggle off/on, verify server tracking unchanged and dedicated
  server client-class isolation. Treat audit distances as approximate until tested.
  Cover both multiplayer with RealSize installed on server and client and
  single-player with the integrated server. Server-only must retain all server
  features with vanilla clients; record unchanged vanilla culling as the expected
  limitation, not a failure of sizing or movement.
- [ ] **RS-02 — Saved scale lifecycle.** Replace permanent/add-if-missing behavior
  with transient reconciliation. Prove config edits reach loaded mobs, current
  settings reach chunk-loaded mobs, neutral/deleted rules remove owned data,
  and repeated load/reload does not stack. Neutral/deleted rules also preserve
  vanilla baby size rather than applying category floors. Inspect save data, strip legacy owned
  attributes, and verify foreign attributes survive. Test uninstall after
  migration; document that untouched legacy chunks still need migration.
- [ ] **RS-03 — Step height and pens.** Replace flat boosts and strict threshold
  with proportional, sub-1.5-capped land movement. Test golems/ravagers against
  fences and walls, camel movement, aquatic exclusion, neutral scaling, and
  pre-existing foreign step modifiers. Numerical bounds alone do not close this.
- [ ] **RS-04 — Configuration truth.** Implement configVersion, authoritative
  entity map, ID/tag selection, warnings, explicit migration, and transactional
  `/realsize reload`. Test deleted defaults, empty maps, typo IDs, bad numbers,
  invalid-file preservation, backup/atomic migration, tag conflicts, and failed
  reload retaining active behavior. Disk schema must match documented semantics.
- [ ] **RS-05 — Hot-path allocation.** Remove tracking injections entirely and
  resolve per-EntityType settings once per registry/config snapshot. Inspect
  adapter paths for per-call string conversions and repeated registry selection;
  profile realistic entities rather than repeat the audit's unmeasured rate.
- [ ] **RS-06 — Hostile size and combat.** Apply the separate hostile floor
  (target default 0.6). Test spider passage under slabs/through holes, cave-spider
  targeting, and melee reach. Document remaining differences; the floor is not
  a promise that all vanilla mob-proofing survives.
- [ ] **RS-07 — Baby size.** For active non-neutral rules, enforce category bounds after baby factors, then
  reconcile growth through the sizing owner. Test baby/adult chickens and other
  ageable mobs, growth, chunk reload, and foreign scale coexistence. Prove neutral
  and absent rules leave vanilla baby sizing untouched.
- [ ] **RS-08 — Enlarged spawn clearance.** Verify the 26.3 natural-placement
  seam and reject collisions using final dimensions. Test a scaled golem in a
  too-short space and an adequate space. Gate any required server mixin on a
  documented architecture decision; do not hide it under a second size system.
- [x] **RS-09 — Build and dev runtime.** Finish the single 26.3 artifact, root
  source set, metadata, and enabled JAR task. Existing tooling groundwork already
  removes old modules/identity mappings; exercise real compile/build/runServer,
  inspect contents and runtime classes, and align naming everywhere. Do not use
  the audit's old version recommendations instead of the project's actual pins.
- [ ] **RS-10 — Dead abstractions.** Do not reintroduce ignored type parameters,
  unused scale getters, constant fallback settings, or pass-through logic owners.
  Review implementation dependencies against ARCHITECTURE; one sizing owner.
- [ ] **RS-11 — Mixin integration.** No server tracking mixins. Verify the sole
  client render adapter's target and injection count; prefer ModifyReturnValue.
  Test transformed code, not source-string assertions.
- [ ] **RS-12 — Compatibility claims.** Declare only tested Minecraft 26.3;
  remove the unverified 1.21.1–1.21.11 range from active metadata/docs. Legacy
  README stays archival and is not current installation guidance.
- [ ] **RS-13 — Source and issue links.** Populate packaged contact metadata for
  `ff-tech-xyz/realsize-mod`. Record and separately deploy approved Modrinth
  source/issue-link corrections, then read them back. No external page changed
  in this documentation task.
- [ ] **RS-14 — Behavioral tests and CI.** Add policy tests, actual Minecraft
  GameTests, a dedicated-server boot, and a CI build/test workflow. Connect both
  vanilla and modded clients for render/movement checks; record real outputs.
  Empty compilation/test tasks are not passing behavior tests.

## Definition of done

Every item has a reproducible acceptance result, not just implementation files.
The packaged JAR starts on a dedicated server, vanilla clients can join, the
optional client correction works, configuration/world migration is exercised,
both modded multiplayer and single-player culling are checked, server-only
features remain functional with vanilla clients,
and CI builds the tested single artifact. Current metadata and documentation
match that result. Any remaining exception is recorded here before release;
release approval is separate from committing documentation.

## State

2026-10-13: recorded the supplied legacy audit as open v2 fixes and replaced the
old architecture/product contract with the finished-state target. Documentation
only; implementation, gameplay verification, and external metadata edits remain
open. The build property remains 1.2.0; v2 is a rebuild name, not a silent bump.

Implementation is now approved: commit the installation-contract clarification,
then dispatch one scaffold worker and a dependency-ordered worker per feature.
Each handoff requires real verification, independent review, and a commit. This
authorization does not mark any gameplay acceptance item complete.

## Executable scaffold evidence — 2026-10-14

Scope: Kanban `t_8b43e409`, branch `update/V.1.2.0`, based on `9c30aee`.
This is build/test/runtime groundwork, not the completed v2 product. The common
and optional client entrypoints log that gameplay/culling are not implemented.
No config/policy placeholders, server mixins, culling declaration, custom
networking, or client-required registries were added.

Pins were retained and resolved successfully: Minecraft `26.3`, Java toolchain
and bytecode `25`, Loom `1.18.4`, Loader `0.19.5`, Fabric API `0.162.0+26.3`,
Gradle `9.8.1` with the existing distribution checksum, mod version `1.2.0`.
The JUnit adapter uses the same pinned Loader version. All build output and
server worlds/logs are under `../../runtime/realsize-mod`, not the checkout.
Loom's test configuration overrides its run directory; the project applies
runtime paths after that configuration, including the separate GameTest run.

Commands run from the repository:

- `./gradlew test --console=plain` before production sources/packaging:
  **BUILD FAILED**, `1 test completed, 1 failed`. The actual artifact assertion
  failed because packaging was disabled and no installable JAR existed.
- `./gradlew clean build --console=plain` after implementation:
  **BUILD SUCCESSFUL**, real main/client/GameTest/test compilation and packaging.
  JUnit XML reports **1 test, 0 skipped, 0 failures, 0 errors**. That test opens
  the built JAR, validates expanded installation metadata and Java 25 entrypoint
  bytecode, compares icon/license bytes to the retained originals, and rejects
  shipping development tests or an unimplemented mixin declaration.
  The real Minecraft server reports **All 2 required tests passed**: the named
  `realsize-test:scaffold_game_tests_scaffold_loads_on_dedicated_server` test and
  Minecraft's built-in `minecraft:always_pass`. This is **one RealSize GameTest**,
  not two feature tests. It verifies the SERVER environment, Loader discovery of
  the actual common entrypoint/version, and an actual spawned bee's untouched
  vanilla scale. The GameTest server saves its world and exits successfully.
- `python3 scripts/server-smoke.py`: starts the real `runServer` dev launch on
  loopback port `25586` in a dedicated disposable smoke directory, accepts the
  development EULA, waits for `Done`, sends `stop`, and enforces a 180-second
  bound with process-group cleanup on failure. Result: **PASS**, common
  initializer logged, server reached startup, saved worlds, and exited **0**.
  JVM `-Xlog:class+load` contains `RealSizeMod`, no RealSize client classes, and
  no `net.minecraft.client.*` classes. It does not substitute a fake server.
- Python `zipfile`, `hashlib`, and `xml.etree.ElementTree` inspection confirmed
  both entrypoint classes, metadata, original icon, and `LICENSE_realsize` in
  `../../runtime/realsize-mod/build/libs/realsize-1.2.0.jar`; no GameTest classes
  are packaged. Metadata declares `environment="*"`, exact Minecraft `26.3`,
  Java `>=25`, required Fabric dependencies, and the real
  `https://github.com/ff-tech-xyz/realsize-mod` source and `/issues` tracker.
  Artifact SHA-256:
  `9b6055d4e844b2748fa10b390e069cacac0ecdfd4dc9aeb33991ae41633f10ad`.
- `git diff --check`: exit **0**. An accessible `/proc` cwd/file-descriptor
  scan found **no references under the RealSize runtime directory** after both
  server runs. No dev server or client is left owning runtime files.

Development failures were fixed, not counted as passing: the first GameTest
compile showed that the verified 26.3 bee constant belongs to `EntityTypes`,
not `EntityType`; `javap` confirmed the actual API. A report filename with no
parent triggered Fabric's XML reporter NPE; `javap` confirmed its unconditional
parent-directory creation, and the configured report now has an absolute
runtime path. An execution-time Gradle `project` lookup in license naming was
moved to configuration time, then the clean build was rerun. Loom/Loader still
warns about the absent, unused client resources directory; there are no client
resources in this scaffold. This warning does not mean a client class loaded.
`NO-SOURCE` remains for intentionally absent client/test resources and access
wideners, not for Java compilation, JUnit, or Minecraft tests.

Reproducible evidence paths under `../../runtime/realsize-mod`:
`evidence/scaffold-red.log`, `evidence/full-build.log`,
`evidence/gametest-report-red.log`, `evidence/server-smoke.log`,
`evidence/server-classes.log`, `build/test-results/test/TEST-xyz.pyrehaven.realsize.ScaffoldTest.xml`,
and `run/gameTest/gametest-results.xml`.
The independently reviewed commit and read-back remote SHA are recorded in the
Kanban handoff; this evidence is included in that commit.

Remaining gates are unchanged: configuration/migration, registry resolution,
sizing policy and lifecycle, reload, movement, spawn clearance, and optional
client culling are unimplemented. RS-01 through RS-08 and RS-10 through RS-14
remain open. Packaged version/contact metadata is groundwork for RS-12/RS-13;
no external Modrinth page was edited. CI execution and its final behavior,
connected vanilla/modded clients, single-player, gameplay/collision/combat,
world/config migration and uninstall checks have **not** been performed. A
passing scaffold boot is not evidence for any of those release gates.
