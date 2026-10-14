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
mapping JAR. Configuration parsing, immutable snapshots, first-run defaults, and
legacy file migration are implemented and tested as a standalone boundary.
Registry/tag resolution is also implemented and tested against the 26.3 server;
entrypoint/lifecycle wiring remains deferred. No gameplay features
are implemented. The former FEATURES.md mentioned
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

At the scaffold commit, configuration/migration, registry resolution, sizing
policy and lifecycle, reload, movement, spawn clearance, and optional client
culling were unimplemented. RS-01 through RS-08 and RS-10 through RS-14 remained
open. Packaged version/contact metadata is groundwork for RS-12/RS-13;
no external Modrinth page was edited. CI execution and its final behavior,
connected vanilla/modded clients, single-player, gameplay/collision/combat,
world/config migration and uninstall checks have **not** been performed. A
passing scaffold boot is not evidence for any of those release gates.

## Configuration and legacy migration evidence — 2026-10-14

Scope: Kanban `t_e6f589f7`, branch `update/V.1.2.0`, base
`1e28d7a6ba091908dacdfdea9fdbc89c627a1540`. `ConfigFile` now owns strict JSON
parsing, schema validation, first-run creation, and explicit unversioned legacy
migration. `RealSizeConfig` is a validated immutable record with a defensive map
copy. `DefaultScales` is used only for missing files; existing maps, including
empty maps, never merge with defaults. Unknown well-formed IDs/tags survive
unchanged for the next feature's registry resolver; this card does not invent
registry warnings, tag precedence/conflict handling, or active settings state.

Migration requires legacy `floor`, `cap`, and `entityScales`. Optional obsolete
tracking/flat-step fields are validated then removed, with a diagnostic naming
the config, backup, schema change, and removed fields. The hostile floor becomes
`max(floor, 0.6)`; incompatible caps fail unchanged. Proportional movement and
the client-local toggle default to true, with `maxStepHeight=1.49`. Explicit
legacy selectors and values remain explicit, not clamped or default-merged.
Missing v2 fields, mixed unversioned/v2 shapes, unsupported versions, duplicate
keys, invalid ID syntax, malformed JSON, wrong scalar types, nonpositive or
non-finite scales, and incompatible bounds all fail with the file and field.
Schema versions are compared as exact decimals, not rounded doubles.

Writes prepare and force a same-directory temporary file. Migration creates
`realsize.json.legacy.bak` with `CREATE_NEW`, writes the original bytes and
forces them before `ATOMIC_MOVE` replacement. There is no non-atomic fallback.
Existing backups are never overwritten; config symlinks are rejected. Failed
preparation/backup leaves the config unchanged and cleans the temporary file.
If replacement itself fails, the original config and completed backup remain.
No concurrent-writer or power-loss/directory-fsync guarantee is claimed.

Default-table review retained all 50 legacy selections. Six hostile entries
were raised to `0.6`: spider, cave spider, silverfish, endermite, vex, and zombie
nautilus. All other requested values remain unchanged. The actual 26.3 server
GameTest resolves every default ID, creates its entity, verifies it is a mob,
and checks the configured category floor/cap. This tests the table; it is not
the production registry-resolution feature or a gameplay/combat size claim.

Commands run from the repository:

- `./gradlew test --tests '*ConfigFileTest' --console=plain`, initial tracer:
  **BUILD FAILED** at compilation because `ConfigFile` did not exist. After
  implementation, **BUILD SUCCESSFUL**, one real JUnit test executed. This
  initial compile failure is not counted as a behavioral assertion failure.
- The same command for strict validation: **4 tests completed, 2 failed** before
  validation; **BUILD SUCCESSFUL** after implementation. Missing required fields
  and zero/invalid scales had previously been accepted. Tests compare original
  file bytes after every rejected input rather than comparing parsed objects.
- The same command for migration: **8 tests completed, 4 failed** before migration
  and symlink handling; **BUILD SUCCESSFUL** afterward. Successful migration
  compares the backup to the exact original bytes, verifies deleted/default
  rules stay absent, and verifies the next read neither rewrites nor re-migrates.
  Existing backup files/directories and unwritable config directories exercise
  real I/O failures. Invalid migrations produce no backup or replacement.
- The same command for exact schema versions: **11 tests completed, 1 failed**
  on `configVersion: 2.0000000000000001` before exact decimal comparison. The
  completed clean build below passes it. Additional checks verify defensive map
  copying and that an already-open handle still reads the legacy inode after
  migration, proving replacement rather than in-place truncation.
- Independent exact-diff review found an extreme-exponent diagnostic losing its
  field path. Added the regression first and ran the same config-test command:
  **12 tests completed, 1 failed** on `1e-2147483648`. Numeric-token conversion
  now reports the reader's field path for unsupported decimal exponents; tests
  also cover `1e2147483648`, exact-byte preservation, and no backup on failure.
- `./gradlew clean build --console=plain`: **BUILD SUCCESSFUL**, real common,
  client, GameTest, and test compilation plus JAR packaging. JUnit XML reports
  **13 tests, 0 failures, 0 errors, 0 skipped**: 12 config tests and the existing
  artifact inspection. The Minecraft server reports **All 3 required tests
  passed**: two RealSize tests (scaffold and default-table validation) and
  vanilla `minecraft:always_pass`. Test temporary files now live under
  `../../runtime/realsize-mod/test-temp`.
- `python3 scripts/server-smoke.py`: **PASS**, real dedicated dev server reached
  `Done`, saved worlds, exited **0**; JVM trace includes `RealSizeMod` and no
  RealSize or Minecraft client classes. Entrypoints remain scaffold-only: config
  loading is exercised by real file tests, not yet wired to active sizing or
  startup resolution. The later registry/lifecycle features own that wiring.
- Python inspection of JUnit/GameTest XML confirmed the counts and named tests.
  Built JAR SHA-256:
  `d40d9ff27f30b036787fbadc84597ef2a45534d87ffb3375c146069cf1cd47fb`.
  `git diff --check`: exit **0**. Accessible `/proc` cwd/fd scan found **no
  runtime references** after the server stopped.

Logs under `../../runtime/realsize-mod/evidence/`: `config-first-red.log`,
`config-first-green.log`, `config-validation-red.log`,
`config-validation-green.log`, `config-migration-red.log`,
`config-migration-green.log`, `config-version-red.log`, `config-clean-build.log`,
`config-review-regression-red.log`, `config-clean-build-reviewed.log`,
`server-smoke.log`, and `server-classes.log`. JUnit and GameTest XML paths are
the same as the scaffold evidence, with the additional
`build/test-results/test/TEST-xyz.pyrehaven.realsize.config.ConfigFileTest.xml`.
Independent exact-diff review, the commit, and read-back remote SHA are recorded
in this card's Kanban handoff, not asserted before they occur.

RS-04 remains open for registry/tag resolution and transactional live reload.
Registry resolution, active startup wiring, sizing policy/lifecycle, movement,
natural-spawn clearance, optional client culling, connected clients,
single-player, world migration/uninstall, CI execution, and final release gates
remain unperformed. No running dev server/client, server mixin, networking,
registry implementation, next-card work, PR, merge, release, production write,
or external Modrinth deployment is part of this feature.

## Registry resolution evidence — 2026-10-14

Scope: Kanban `t_b5b614a0`, branch `update/V.1.2.0`, base
`bfec907ca4440b85f701b47d53fe4d7c2d94f157`. The first feature commit attempt was
blocked by the architecture hook: newly added test paths must be declared in an
earlier groundwork commit. Independently reviewed layout-only commit
`7c60ab6439ee1a4a2e5d5284f85c8633020eb724` now declares those paths; no hook was
bypassed. `ResolvedSettings` resolves a validated
`RealSizeConfig` against the actual server's `Registry<EntityType<?>>`. It owns
a defensively copied, immutable identity-keyed map of requested scales and the
immutable config inputs. `scale(EntityType<?>)` returns the preboxed requested
value, including explicit neutral `1.0`, or `null` for no eligible rule. It does
not clamp, apply attributes, select category/baby limits, or activate global state.

Exact IDs take precedence. Differing tag values fail only where two effective
tag rules select the same eligible type without an exact override; equal tag
values agree. Diagnostics name both selectors and the conflicting entity ID.
Selector processing is sorted, not dependent on input-map iteration order.
Unknown well-formed IDs/tags each emit a named warning and remain in the config
and on disk unchanged. Known empty tags are not unknown. Players and nonliving
types are excluded from both exact and tag selection before conflict checks;
living non-Mob types such as armor stands remain eligible.

Verified API seam: `javap` on the actual 26.3 common JAR shows
`EntityType.getBaseClass()` unconditionally returns `Entity.class`, so that API
cannot classify living types. Resolution uses `DefaultAttributes.hasSupplier`
with an explicit player exclusion instead. A GameTest selects every registered
type, creates actual entities, and checks that eligibility matches
`LivingEntity` membership for every non-player type. This creates no production
entities during resolution and adds no new registry, networking, or mixin.

Commands run from the repository (logs under `../../runtime/realsize-mod/evidence/`):

- `./gradlew runGameTest --console=plain`, initial exact-ID tracer:
  **BUILD FAILED** at compilation because `ResolvedSettings` did not exist.
  This is not counted as an assertion failure. After minimal implementation,
  the first run passed only the old 3 tests: the new test class was missing from
  the explicit development entrypoint list. That run is not resolution evidence.
  Registering the class and rerunning produced **All 4 required tests passed**,
  including the named exact-rule test. Logs: `resolution-first-red.log`,
  `resolution-first-green.log`, `resolution-first-registered-green.log`.
- Same command, tag precedence: **1 required test failed** out of 5, because the
  selected wither-skeleton tag member returned no scale. After tag resolution,
  **All 5 required tests passed**. Logs: `resolution-tags-red.log` and
  `resolution-tags-green.log`.
- Same command, conflicting tags: **1 required test failed** out of 6 with
  `Conflicting tag rules must fail rather than choose a winner`. After explicit
  conflict detection, **All 6 required tests passed**. Logs:
  `resolution-conflicts-red.log` and `resolution-conflicts-green.log`.
- Same command, exclusions: **1 required test failed** out of 7 with
  `Tag must exclude players`. After eligibility filtering, **All 7 required
  tests passed**, including the registry-wide actual-entity check. Logs:
  `resolution-exclusions-red.log` and `resolution-exclusions-green.log`.
- Same command, unknown selectors: **1 required test failed** out of 8 with
  `Both unknown selectors must warn exactly once`. After named warnings,
  **All 8 required tests passed**. The file test loads real JSON, resolves it,
  compares exact original bytes afterward, and verifies no unknown-ID fallback
  to the default pig type. Logs: `resolution-unknown-red.log` and
  `resolution-unknown-green.log`.
- Same command, additional snapshot/overlap coverage: **All 10 required tests
  passed**. These additional coverage tests passed on first execution, not a
  claimed red cycle. The snapshot test changes the actual server registry's tag
  bindings using `prepareTagReload(...).apply()`, verifies the old snapshot stays
  fixed while a fresh resolution observes the new binding, and restores all
  original tag bindings in `finally`. It also checks mutable input-map isolation
  and an empty replacement with no default merge. This is test-only rebinding,
  not production reload wiring. Log: `resolution-snapshots.log`.
- `./gradlew clean build --console=plain`: **BUILD SUCCESSFUL**, actual common,
  client, GameTest, and JUnit compilation and JAR packaging. XML inspection:
  **13 JUnit tests, 0 failures, 0 errors, 0 skipped** (the existing 12 config
  tests and artifact test); **10 required Minecraft tests passed** (7 new
  resolution tests, 2 existing RealSize tests, vanilla `minecraft:always_pass`).
  Final coverage additionally checks known empty tags, ignored nonliving tag
  overlaps, and that a failed candidate leaves a prior snapshot unchanged.
  Log: `resolution-clean-build.log`. The existing absent-client-resource warning
  remains; no Java/test task is `NO-SOURCE`.
- `python3 scripts/server-smoke.py`: **PASS**, normal dedicated dev server
  initialized RealSize, reached `Done`, saved worlds, exited **0**; class-load
  trace has no RealSize or Minecraft client classes. Logs: `server-smoke.log`
  and `server-classes.log`. Entrypoints remain scaffold-only; this is not active
  startup config/resolver wiring.
- `javap -classpath ../../runtime/realsize-mod/build/classes/java/main -c
  xyz.pyrehaven.realsize.config.ResolvedSettings` confirms `scale` only reads the
  map, invokes `Map.get`, casts the existing `Double`, and returns it. No ID
  conversion, string construction, registry lookup, boxing, or allocation occurs
  in this lookup method. No gameplay-path profiler measurement is claimed.
- Python JAR inspection confirms the resolver is packaged and no GameTest class
  or test-only tag data ships. JAR SHA-256:
  `9778694020e0df22e20485478598d7cf03ca41165a156324d3068d10248ff915`.
  `git diff --check`: exit **0**. Accessible `/proc` cwd/fd scan found **no runtime
  references** after shutdown.

Reports: `../../runtime/realsize-mod/build/test-results/test/TEST-*.xml` and
`../../runtime/realsize-mod/run/gameTest/gametest-results.xml`. Development-only
`only_bee`, `mixed`, and `empty` tags exercise real server datapack tag loading;
they are declared in ARCHITECTURE and only registered in GameTest metadata.
Independent exact-diff review, commit SHA, and read-back remote SHA belong in the
Kanban handoff after those actions occur; this evidence is committed with the code.

RS-04 remains open for active startup/lifecycle integration and transactional
reload. RS-05 has a verified allocation-free lookup, but gameplay-path integration
and profiling remain unperformed. Sizing policy, entity mutation/legacy world
attribute cleanup, movement, spawn clearance, client culling, connected clients,
single-player, world migration/uninstall, CI execution, and final release gates
remain unperformed. No running dev server/client, production reload, sizing
adapter, server mixin, networking, new registry, next-card implementation, PR,
merge, release, production write, or external Modrinth deployment is part of this
feature.
