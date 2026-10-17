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
pure category/baby-aware sizing policy is implemented and unit-tested.
Transient entity sizing, legacy attribute cleanup, and common server load/growth
wiring are implemented and exercised against actual mobs and chunk storage.
Transactional command/tag reload and changed-config chunk loading are implemented
and tested. Proportional movement and actual server collision courses are also
implemented and tested. Spawn clearance and optional client culling remain
deferred. The former FEATURES.md mentioned
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

## Category and baby sizing policy evidence — 2026-10-14

Scope: Kanban `t_0424bb98`, branch `update/V.1.2.0`, base
`e80c1682d1a2369f5e812f63eac938849bce2d38`. `SizingPolicy` is a pure immutable
calculation with numeric bounds and no Minecraft, Fabric, entity, or file imports.
The adapter supplies validated configured bounds (defaults `0.22`, `0.6`, `1.45`),
the resolved requested scale or `null`, hostile classification, and the vanilla
age factor excluding owned/foreign attributes. It returns an owned multiplier,
not an attribute amount. For active non-neutral rules:
`clamp(requestedScale * babyFactor, categoryFloor, cap) / babyFactor`.
Neutral `1.0` and absent rules return `1.0`, meaning remove the owned modifier;
they preserve vanilla baby size even below a category floor. Entity mutation,
category/age API integration, foreign-mod diagnostics, and growth callbacks
remain the next lifecycle feature, not a claim made by numerical tests.

Commands run from the repository (logs under `../../runtime/realsize-mod/evidence/`):

- `./gradlew test --tests '*SizingPolicyTest' --console=plain`, first tracer:
  **BUILD FAILED** at compilation because the policy did not yet exist. After
  the neutral-only implementation, **BUILD SUCCESSFUL**, one actual test.
  Logs: `sizing-first-red.log`, `sizing-neutral-green.log`. An earlier command
  launched without an explicit working directory failed before Gradle or tee
  ran; it is not test evidence. All subsequent commands set the repository cwd.
- Same command, active adults: **2 tests completed, 1 failed** before category
  clamping; **BUILD SUCCESSFUL** afterward. Logs: `sizing-adult-red.log`,
  `sizing-adult-green.log`.
- Same command, active babies: **3 tests completed, 1 failed** when clamping the
  requested multiplier before the baby factor; **BUILD SUCCESSFUL** after
  bounding the final adult-relative size. Logs: `sizing-baby-red.log`,
  `sizing-baby-green.log`. Cases cover ordinary/hostile baby floors, an active
  enlargement within the final cap, and enlargement reaching the final cap.
- Same command, validation: **6 tests completed, 1 failed** before positive,
  finite input/bound validation. Log: `sizing-validation-red.log`. Additional
  tests for exact/adjacent category boundaries, custom/equal bounds, growth
  recalculation, and extreme requested scales passed on first execution; these
  are coverage, not claimed red cycles. Unrepresentable owned multipliers fail
  explicitly rather than returning infinity.
- `./gradlew clean build --console=plain`: **BUILD SUCCESSFUL**, 13 actionable
  tasks (11 executed, 2 up-to-date), real source/test compilation and packaging.
  XML reports **19 JUnit tests, 0 failures, 0 errors, 0 skipped**, including
  **6 new sizing tests**. The actual Minecraft server reports **All 10 required
  tests passed**, all existing scaffold/config/registry tests plus the vanilla
  test; no new entity-sizing GameTest is claimed. Log: `sizing-clean-build.log`.
  The existing absent client-resource warning remains; Java/JUnit/GameTest
  execution is not `NO-SOURCE`.
- `python3 scripts/server-smoke.py`: **PASS**, dedicated server initialized,
  reached `Done`, saved worlds, exited **0**, and loaded no RealSize or Minecraft
  client classes. Logs: `server-smoke.log`, `server-classes.log`. Entrypoints
  remain scaffold-only, so this is isolation/startup evidence, not active sizing.
- Python XML/JAR inspection verified the test counts and packaged
  `xyz/pyrehaven/realsize/sizing/SizingPolicy.class`. JAR SHA-256:
  `4fa6a0d35d258bb0067e370e41e556a6551f7c6f5ffd370e43780f01ae393f4b`.
  `git diff --check`: exit **0**. Accessible `/proc` cwd/fd scan found **no
  runtime references** after shutdown.

Report: `../../runtime/realsize-mod/build/test-results/test/TEST-xyz.pyrehaven.realsize.sizing.SizingPolicyTest.xml`.
Independent exact-diff review, commit SHA, and read-back remote SHA are recorded
in this card's Kanban handoff after they occur; this evidence ships in the commit.
RS-06/RS-07 remain open for actual mobs, combat/collision, growth/chunk lifecycle,
and foreign attributes. Active sizing, legacy saved-attribute cleanup, reload,
movement, spawn clearance, optional culling, gameplay profiling, connected clients,
single-player, world migration/uninstall, CI, and release gates remain unperformed.
No movement formula or Minecraft attributes, next-card implementation, running
server/client, server mixin, networking, new registry, PR, merge, release,
production write, or external deployment is part of this feature.

## Transient entity sizing lifecycle evidence — 2026-10-14

Scope: Kanban `t_34417d51`, branch `update/V.1.2.0`, base
`b374895030c48f15513be961d9e4adf960022962`. `EntitySizing` is the sole attribute
mutation owner. It takes the immutable server-registry snapshot and pure policy,
removes permanent modifiers in the exact `realsize` namespace from every
instantiated living attribute on load, and applies `realsize:scale` using
`ADD_MULTIPLIED_TOTAL` as a transient modifier. It never changes base attributes
or foreign modifiers. An unchanged reconciliation retains the same modifier
object and does not dirty the attribute. A replacement snapshot updates that ID;
neutral/deleted/excluded rules remove it. Legacy step data is removed, but no new
movement behavior is implemented here. A foreign/base scale that defeats the
bounds is diagnosed when the owned scale is applied or changed, without a
per-entity diagnostic state map or repeated warnings on every unchanged tick.

Verified 26.3 APIs, inspected with `javap` against the actual resolved common and
Fabric JARs: `LivingEntity.getAgeScale()` provides the vanilla age factor;
`getScale()` reads the scale attribute without that factor; default dimensions
multiply both. Attribute packing exposes all instantiated attributes and only
their permanent modifiers, including legacy data on non-syncable attributes.
`removeModifier` returns before dirtying when the ID is absent. No attribute or
modifier packing occurs in the steady-state tick path.

`RealSizeMod` loads/validates/resolves once at `SERVER_STARTING`, delegates
`ServerEntityEvents.ENTITY_LOAD` to the owner, and reconciles loaded entities at
`ServerTickEvents.END_LEVEL_TICK`. The pinned Fabric API has no growth callback;
the level-end event runs after vanilla age changes without a second age cache,
entity state map, or server mixin. These are common server callbacks, not a
dedicated-only initializer or separate integrated-server implementation. The
normal dedicated server has exercised startup; an interactive single-player
session remains an explicit release gate, not a claimed test result. Startup
configuration is necessary wiring for this feature; command and tag reload are
still deferred to the next card.

Commands run from the repository (logs in `../../runtime/realsize-mod/evidence/`):

- `./gradlew runGameTest --console=plain`, first tracer: **BUILD FAILED** because
  `EntitySizing` did not exist. After transient reconciliation was implemented,
  **All 11 required tests passed**. Logs: `lifecycle-first-red.log` and
  `lifecycle-first-green.log`. The initial failure is compilation, not a claimed
  behavioral assertion failure.
- Same command, legacy cleanup: **1 required test failed** of 13, naming
  `Remove persistent realsize scale, preserve foreign scale`. After cleanup,
  **All 13 required tests passed**. Logs: `lifecycle-cleanup-red.log` and
  `lifecycle-cleanup-green.log`. Tests inspect scale, max-health, and step
  attributes; preserve foreign/prefix-similar namespace modifiers, transient
  non-scale modifiers, and exact base values; and serialize an actual baby.
- Same command, registered lifecycle: **1 required test failed** of 14, naming
  `Registered entity-load callback must bound actual baby chicken`. After root
  wiring, **All 14 required tests passed**. Logs: `lifecycle-wiring-red.log` and
  `lifecycle-wiring-green.log`. A chicken ages naturally from `-2` to adult;
  the owned multiplier changes through the registered tick event. Additional
  checks cover an actual hostile baby, neutral cow baby, absent-rule pig baby,
  and actual collision width after growth.
- Same command, real chunk save/load: **All 15 required tests passed** after
  fixing the test harness. Log: `lifecycle-chunk-loaded.log`. Earlier harness
  failures are retained, not called behavioral red cycles: `ChunkPos` is a record
  using `containing`/accessors; dimension storage is under
  `world/dimensions/minecraft/overworld`, obtained with `DimensionType.getStorageFolder`;
  chunk entities must finish asynchronous loading/tracking before injecting
  genuine legacy data. Saving during pending loads legitimately invoked cleanup
  before that fixture could persist (`lifecycle-chunk-debug.log`). The corrected
  test waits on real load/ticking status instead of guessing a delay.
- `./gradlew clean build --console=plain`: **BUILD SUCCESSFUL**, 13 actionable
  tasks, 11 executed and 2 up-to-date. XML inspection: **19 JUnit tests, 0 failures,
  0 errors, 0 skipped**; **16 required Minecraft tests passed**, including **6 new
  sizing GameTests**, 9 prior RealSize tests, and vanilla `minecraft:always_pass`.
  Log: `lifecycle-clean-build.log`. The fixture initializer exists only in the
  separate development mod and writes its disposable GameTest config. No Java
  compilation or behavioral test is `NO-SOURCE`; the existing absent client
  resources warning remains.
- `python3 scripts/server-smoke.py`: **PASS**, real dedicated server initialized
  the sizing owner, reached `Done`, saved worlds, and exited **0**. Class-load
  trace contains `RealSizeMod` and `EntitySizing`, with no RealSize or Minecraft
  client classes. Logs: `server-smoke.log` and `server-classes.log`.
- Python XML/JAR inspection verifies the test counts, packaged sizing owner,
  and absence of GameTests/test resources in the production artifact.
  `git diff --check`: exit **0**. Accessible `/proc` cwd/fd inspection reports
  **0 runtime process references** after shutdown.

The chunk GameTest writes a genuine persistent legacy fixture to entity-region
storage, reads its on-disk NBT, waits for removal reason `UNLOADED_TO_CHUNK` and an
absent chunk, then reloads the chunk and verifies a different entity object with
the same UUID. The baby remains a baby and has final adult-relative size `0.264`
after its preserved foreign `1.2` multiplier. Resaving and reading the region
proves that all permanent legacy RealSize data is gone and no transient RealSize
data was serialized. The foreign modifier and scale/step/movement bases remain.
On-disk NBT extracts are under
`../../runtime/realsize-mod/run/gameTest/sizing-evidence/` as `chunk-legacy.nbt`,
`chunk-legacy.snbt`, `chunk-migrated.nbt`, and `chunk-migrated.snbt`. These are
actual saved data, not generated stand-ins. XML reports retain their usual paths.
Independent exact-diff review, commit SHA, and remote read-back SHA are recorded
in the Kanban handoff after those operations occur.

RS-02 remains open for command/tag reload, changed-config chunk loading, and
uninstall/untouched legacy chunks. RS-06/RS-07 now have actual category/baby/growth
and chunk tests, but combat and broader gameplay remain unperformed. Movement,
natural spawn clearance, optional client culling, connected vanilla/modded
clients, interactive single-player, gameplay profiling, CI, and release gates
remain open. No next-card implementation, server mixin, custom networking,
client-required registry, running dev server/client, PR, merge, release,
production change, or external deployment is part of this feature.

## Transactional configuration and tag reload evidence — 2026-10-14

Scope: Kanban `t_524dcc8f`, branch `update/V.1.2.0`, base
`cb5f52bfba48adc982ebb62eb3b603dc89451e9c`. The registered `/realsize reload`
command requires Minecraft's level-2 `LEVEL_GAMEMASTERS` permission check. The
composition root prepares a complete immutable resolved settings/policy owner,
then replaces its single active owner reference and reconciles every loaded
entity in every server level. Activation rejects a non-server-thread call.
Attribute mutation still belongs only to `EntitySizing`; no entity state map,
parallel sizing handler, networking, server mixin, or new registry was added.

`ConfigFile.loadOrCreate` can now validate/resolve the complete candidate before
first-run or migration writes. Parsing, schema, registry/tag conflict, or write
failure prevents activation; invalid input stays byte-for-byte on disk. In
particular, a legacy file with conflicting tag selections is not migrated and
does not acquire a backup before resolution succeeds. Existing callers retain
the same parse-only API. Startup uses the same full candidate preparation and
still fails on an invalid configuration instead of silently using defaults.

Verified API seam: `javap -c -p` against the pinned Fabric lifecycle JAR shows
`END_DATA_PACK_RELOAD` dispatched by `handleAsync(..., MinecraftServer)` after
the real resource reload future finishes, with a success flag. The GameTest
observes the transformed callback on the actual server thread. Successful
datapack reload re-resolves the active immutable config against the current
server registry/tag bindings and uses the same activation/reconciliation path.
It deliberately does not read uncommitted disk edits. A tag conflict logs the
entity ID and both selectors and retains prior resolved membership and sizing;
Minecraft's already-committed tags are not rolled back by RealSize. Failed
Minecraft resource reloads do not activate a candidate.

Commands run from the repository, with logs under `../../runtime/realsize-mod/evidence/`:

- `./gradlew runGameTest --console=plain`, registered-command tracer:
  **BUILD FAILED**, **1 of 17 required tests failed** because the command was
  absent. Log: `reload-command-red.log`. The first implemented run also failed
  the diagnostic assertion: the fixture's zero `floor` correctly reported
  `floor`, not the test's assumed missing field. That harness expectation was
  corrected, not claimed as a production fix. Logs: `reload-command-green.log`
  (failed) and `reload-command-passed.log` (**All 17 required tests passed**).
- Same command, tag integration with the callback deliberately absent:
  **BUILD FAILED**, **1 of 18 required tests failed**, `Changed tags must
  immediately remove/add loaded sizing`. Log: `reload-tags-red.log`. Restoring
  the callback initially exposed a test-isolation problem: the failed run's
  datapack remained selected in the reused GameTest world, causing four tests
  to fail (`reload-tags-green.log`). The development initializer now removes
  only its own stale fixture before world loading. The successful test restores
  selected packs/config and removes its fixture. Corrected run:
  **All 18 required tests passed**, `reload-tags-passed.log`.
- `./gradlew clean build --console=plain`: **BUILD SUCCESSFUL**, **13 actionable
  tasks: 11 executed, 2 up-to-date**. Actual common/client/GameTest/test Java
  compilation and packaging ran. XML inspection reports **19 JUnit tests, 0
  failures, 0 errors, 0 skipped**, and **18 required Minecraft tests passed**:
  two new reload GameTests, 15 prior RealSize tests, and `minecraft:always_pass`.
  Log: `reload-clean-build.log`. Intentionally absent resources/access wideners
  still print `NO-SOURCE`; Java and behavioral tests do not.
- `python3 scripts/server-smoke.py`: **PASS**, actual dedicated server initialized
  RealSize, reached `Done`, saved worlds, and exited **0**. JVM trace contains
  the common root/owner and no RealSize or Minecraft client classes. Logs:
  `server-smoke.log` and `server-classes.log`.
- `git diff --check`: exit **0**. Python XML/JAR inspection confirms the packaged
  production code and absence of GameTest classes/resources. Accessible `/proc`
  cwd/file-descriptor inspection finds **0 runtime process references** after
  shutdown. Artifact SHA-256:
  `4bc1f6382eb7495f8f304a571e7f0b742b399dac5faf6aa3ae1e49beaba6d88e`.

The command test executes the registered Brigadier dispatcher with actual
Minecraft level-0, level-1, and level-2 permission sets. Levels 0/1 cannot execute;
level 2 updates a loaded bee before the command returns. Malformed v2 input,
conflicting v2 tag rules, and conflicting legacy tag rules preserve exact disk
bytes, loaded modifier identity, and the snapshot used by subsequently loaded
mobs. Diagnostics name the file and failing field. Deleting the bee rule and
setting chicken to neutral removes owned scale and preserves vanilla baby size.

The datapack test writes real pack metadata and tag JSON under the disposable
world, discovers/enables the pack, and calls actual `MinecraftServer.reloadResources`.
Moving tag membership from bee to cow immediately removes bee sizing and applies
cow sizing; adding a conflicting tag preserves previous loaded/newly loaded cow
behavior; fixing the pack recovers. Four real resource reloads execute, including
restoration of original packs. Invalid uncommitted config bytes remain unchanged
throughout. This is transformed Fabric/Minecraft integration, not direct test-only
event invocation or registry rebinding standing in for reload wiring.

The existing chunk-region test now reloads config while its saved baby chicken
is genuinely unloaded (`UNLOADED_TO_CHUNK`, absent chunk/entity). Reload does not
force that chunk to load. Loading it afterward creates a distinct object with
the same UUID and applies the new `0.8` rule, giving final adult-relative scale
`0.48` after its preserved foreign `1.2` multiplier. Actual on-disk region NBT
still proves legacy cleanup, foreign preservation, and no transient owned
serialization. Evidence remains in `run/gameTest/sizing-evidence/chunk-*.nbt`
and `.snbt`. This supersedes the earlier lifecycle test's `0.264` expectation;
the earlier evidence section records that commit's actual result.

Reports remain `build/test-results/test/TEST-*.xml` and
`run/gameTest/gametest-results.xml` under the runtime root. Independent exact-diff
review, commit SHA, and read-back remote SHA are recorded in the Kanban handoff
after those operations occur; this evidence is included in the feature commit.
RS-04's local configuration/reload contract is now exercised. Its release-wide
acceptance, RS-02 uninstall/untouched legacy chunks, movement and collision/combat
gates, natural spawn clearance, optional culling, connected vanilla/modded clients,
interactive integrated-server single-player, gameplay profiling, CI execution,
and release validation remain open. No next-card implementation, running dev
server/client, PR, merge, release, production mutation, or public deployment is
part of this feature.

## Proportional movement and collision evidence — 2026-10-14

Scope: Kanban `t_7826bd1c`, branch `update/V.1.2.0`, base
`e8065d0609a48ef8130c6b8e2a754ea12570bfa9`. Movement belongs to the existing
`EntitySizing` owner and pure `SizingPolicy`; no new event, entity map, mixin,
networking, or registry was added. Existing load/tick/reload paths use that owner.
The documented proportional formula uses an effective ceiling that survives
Minecraft's float conversion. Default `1.49` is unchanged; a valid double
immediately below `1.5` must not round up to `1.5f`. Higher foreign/vanilla
baselines remain untouched, including the camel's `1.5`.

`realsize:step_height` is transient and uses `ADD_MULTIPLIED_TOTAL` to provide
the exact effective difference from baseline even with foreign additive/base/
total operations. Baseline excludes that ID and uses Minecraft's operation order
and sanitization. An unchanged owned modifier is not removed/readded or dirtied.
Shrinking never lowers baseline; neutral/deleted rules, disabled movement,
non-Mob entities and aquatic mobs remove owned movement. Aquatic categories plus
fish/guardians/nautilus families cover hostile/MISC exceptions. Sizing remains
enabled for aquatic mobs.

`javap -c -p` on the resolved 26.3 common JAR verified `maxUpStep()` converts
the attribute to float (with a ridden-player minimum of `1.0`) and
`AttributeInstance.getModifiers()` makes an immutable set copy. The owner uses
that public snapshot only when its step modifier exists; no allocation-free or
profiling claim is made. Bytecode evidence is in `evidence/movement-api.txt`,
`movement-attribute-bytecode.txt`, and `movement-living-bytecode.txt` under the
runtime root.

Commands run from the repository; logs under `../../runtime/realsize-mod/evidence/`:

- `./gradlew test --tests '*SizingPolicyTest' --console=plain`: initial
  `movement-policy-red.log` **BUILD FAILED** at compilation because `stepHeight`
  did not exist, not a behavioral assertion failure. After implementation,
  `movement-policy-green.log` **BUILD SUCCESSFUL**, **7 policy tests** executed.
- `./gradlew runGameTest --console=plain`: `movement-owner-red.log` **1 of 19
  required tests failed**, `Enlarged golem must receive proportional movement`.
  After integration, `movement-owner-green.log` **All 19 passed**. Twenty repeated
  reconcile/load passes retain modifier identity/base; actual serialization
  excludes owned movement/scale; neutral/deletion restore baseline.
- Same GameTest command: `movement-aquatic-red.log` **1 of 21 failed** on
  `Aquatic exclusion must remove movement: minecraft:cod`. After exclusion,
  `movement-aquatic-green.log` **All 21 passed**. Thirteen actual aquatic types
  retain sizing and lose even stale transient owned step data. Foreign-operation,
  toggle and camel tests passed first run (coverage, not a claimed red cycle).
  Foreign changes reconcile while own data exists; modifier objects/base survive.
- Same GameTest command: `movement-collisions-first.log` **All 23 passed**, but
  unregistered fixtures initially retained unscaled hitboxes. That initial pass
  is not accepted enlarged-hitbox evidence. Fixtures now call vanilla
  `refreshDimensions()` and assert width equals type width times scale/age before
  movement. Corrected full builds below pass these actual enlarged-hitbox checks.
- Policy command: `movement-float-red.log` **8 tests completed, 1 failed**.
  `Math.nextDown(1.5)` became `1.5f`. The effective ceiling now uses the greatest
  float below `1.5`, preserving higher baseline exceptions.
- With that ceiling deliberately removed, the GameTest command failed **1 of
  23** in `movement-fence-rounding-red.log`. A second run separating the movement
  assertion, `movement-fence-escape-red.log`, recorded actual fence escape:
  `Closest legal double cap must block actual fence escape; displacement=12.0`.
  Restoring the ceiling fixes real collision, not just a numerical assertion.
- `./gradlew clean build --console=plain`: corrected `movement-clean-build.log`
  and final `movement-final-build.log` both **BUILD SUCCESSFUL**, **13 actionable
  tasks: 11 executed, 2 up-to-date**. Final XML: **21 JUnit tests, 0 failures, 0
  errors, 0 skipped** (8 policy tests); **23 required Minecraft tests passed**
  (5 new movement tests, 17 prior RealSize tests, vanilla test). Actual Java
  compilation/packaging ran; `NO-SOURCE` only for absent resources/access wideners.
- `python3 scripts/server-smoke.py`: **PASS**, real dedicated server initialized,
  reached `Done`, saved worlds, exited **0**, and loaded no RealSize/Minecraft
  client classes. Logs: `server-smoke.log`, `server-classes.log`.
- Python XML/JAR inspection confirms counts and no packaged GameTest classes/tags.
  Artifact SHA-256:
  `bf69a595be0705b1d926ccbbc23378c31581123cd0d16a9c6669d317fa291712`.
  `git diff --check`: **0**. Accessible `/proc` cwd/fd scan: **0 runtime references**.

Courses use loaded stone ground, real oak fences/connected cobblestone walls,
enlarged golem/ravager hitboxes, and 48 actual Minecraft
`Entity.move(SELF, Vec3(0.25,-0.08,0))` calls per traversal. Each type at `1.01`,
`1.2`, `1.45` tests vanilla baseline, foreign `1.2` baseline/default cap, and
foreign `1.2` baseline/closest legal double cap: **36 blocked traversals**.
Twelve positive controls cross the same courses with higher foreign steps,
proving the course is passable and those baselines are preserved. Controls travel
`12.0` blocks. Enlarged ravager `1.45` stops at `2.9612499475479126` against fence
and `2.8987499475479126` against wall. Near-limit owned step is `1.4999999f`.
Actual measurements: `run/gameTest/sizing-evidence/movement-collisions.txt`.
A stone plus three snow layers course has collision height `1.25`: enlarged
golems/ravagers cross where vanilla versions cannot, while vanilla/enlarged camel
both cross with its original `1.5` step.

The card's local server collision gate is exercised, not left at numerical bounds.
RS-03 remains open for broader player-driven gameplay, AI/pathfinding and
connected-client checks; deterministic actual movement is not an interactive play
session. Connected vanilla/modded clients, integrated single-player, wider
combat/mob-proofing, profiling, uninstall/untouched legacy chunks, CI execution,
spawn clearance, optional culling, and release gates remain unperformed.
No next-card implementation, dev server/client, PR, merge, release, production
mutation or public deployment is included. Independent exact-diff review, commit
SHA and remote read-back SHA are recorded in the Kanban handoff after those
operations occur; this evidence is committed with the implementation.

## Natural placement seam blocker — 2026-10-14

Scope: Kanban `t_ef0c50ef`, branch `update/V.1.2.0`, parent commit
`4fe7390a46695276ed47fe696c01290ea77228f3`. **Blocked, not implemented.**
No production `SpawnSizing`, server mixin, or second sizing model was added.
At the time of this spike the zero-server-mixin contract remained unchanged
pending Elijah's decision. The approval below supersedes that architecture gate.

An executable, development-only spike registered Fabric's `ALLOW_LOAD`, called
the existing `EntitySizing` owner with an iron-golem rule of `1.45`, refreshed
the actual dimensions, and tested world collision before entity registration.
It called real `SpawnUtil.trySpawnMob` using the villager path's
`MOB_SUMMONED`, `LEGACY_IRON_GOLEM`, and final boolean `false`; horizontal and
vertical search ranges were zero for a deterministic candidate. This is the
natural village-golem placement path, not a claim that the reason is `NATURAL`
or that all natural spawn mechanisms were exercised.

- `./gradlew runGameTest --console=plain`: **BUILD FAILED**, exit **1**;
  **24 Minecraft tests, 1 failure**, only the seam spike. Adequate space invoked
  the transformed callback once, accepted a golem, returned a present Optional,
  and registered that exact UUID/object. A three-block roof permits vanilla
  dimensions but collides with the actual enlarged height
  `3.9150002002716064`. There the callback ran once, returned false, and prevented
  registration; nevertheless `trySpawnMob` returned a **present Optional**.
  The final assertion failed: `Rejected scaled placement must return empty to
  natural golem caller, not report success`. This is a real failing contract
  check, not an accepted spawn-clearance implementation.
- `javap -p -c` against the pinned 26.3 common JAR confirms `SpawnUtil` invokes
  the void `addFreshEntityWithPassengers`, then plays the ambient sound and
  unconditionally returns `Optional.of(mob)`. The villager checks `isEmpty()`
  before its success continuation. A load veto therefore stops candidate retries
  while reporting success. `ALLOW_LOAD` occurs at entity-manager insertion,
  too late to repair that control flow.
- Pinned object-builder API inspection shows `FabricEntityType.Builder.Mob`
  offers `spawnPlacement` for entity-type construction. This golem path instead
  calls instance `checkSpawnRules`/`checkSpawnObstruction`, not `SpawnPlacements`.
  No suitable earlier public callback was found in the inspected pinned APIs.
  This is not a claim about every future Fabric release.
- Independent exact-spike review by Hermes subagent
  `gpt-6.1-sol/openai-codex`, `deleg_36f04453/task-0`, returned **BLOCKED** and
  independently confirmed the bytecode, runtime evidence, and object-builder
  limitation. No READY receipt was recorded.
- The throwaway source and temporary test-entrypoint wiring were removed after
  preserving the reproducer. `./gradlew clean build --console=plain` then
  reported **BUILD SUCCESSFUL**, 13 actionable tasks (11 executed, 2 up-to-date).
  XML inspection: **21 JUnit tests, 0 failures/errors/skipped** and **23 Minecraft
  tests, 0 failures/errors**. This verifies restored passing groundwork, not
  spawn clearance. Accessible `/proc` cwd/fd inspection found **0 runtime process
  references** after shutdown. `git diff --check` passed.

Evidence under `../../runtime/realsize-mod`: `evidence/SpawnGameTests.java`,
`evidence/spawn-seam-spike.log`, `evidence/spawn-seam-spike-results.xml`,
`run/gameTest/sizing-evidence/spawn-seam-spike.txt`,
`evidence/spawn-util-bytecode.txt`, `evidence/spawn-villager-bytecode.txt`,
`evidence/spawn-natural-bytecode.txt`, `evidence/spawn-object-builder-bytecode.txt`,
and `evidence/spawn-spike-restored-build.log`. Restored XML reports retain their
normal paths. The reviewer transcript is
`/home/p-h-r-e-d/phred2.0/home/cache/delegation/live/deleg_36f04453/task-0.log`.

The decision requested at that point was to approve a narrowly scoped server spawn-placement
adapter exception before insertion/success reporting, delegating mutation to
`EntitySizing` and preserving failed-candidate retries; or retain zero server
mixins and explicitly defer the spawn-clearance guarantee. A golem-only seam
would not by itself complete all natural-spawn integration. RS-08 and the
dependent feature chain remain open. No feature commit/push or release occurred;
this blocker evidence was left as an uncommitted FIXES.md update. Existing-mob,
explicit-summon and reload non-relocation checks for a future adapter, connected
clients, single-player, profiling, uninstall, CI, and release gates remain
unperformed. No server/client remains running and no next card was implemented.

## Approved spawn-placement architecture — 2026-10-15

Elijah approved the narrow exception with `yes keep building` in Discord
message `1560310593595117589`, replying to the scoped approval question. README
and ARCHITECTURE now permit only pre-success spawn-placement mixins, preserving
failed-candidate retries and the single `EntitySizing` mutation owner. Server
tracking mixins, general load vetoes, networking and client-required registries
remain prohibited. SpawnUtil and NaturalSpawner adapters and their common
required mixin configuration are declared before production source is added.
Finalized candidate state must be sized before acceptance; finalization, if
reordered within a placement loop, still runs once and preserves success/group
semantics. Explicit summons and existing/reloaded entities are not relocated or
deleted to resolve collisions.

Groundwork only: no production spawn adapter has been added at this point.
The failed 2026-10-14 spike and restored passing build above remain historical
evidence, not acceptance of the new implementation. Local/connected gameplay,
single-player, profiling, uninstall, CI and release gates remain open. The
architecture groundwork is independently reviewed and committed before source
implementation; its commit/review identifiers belong in the Kanban handoff.

## Finalized-candidate transaction blocker — 2026-10-15

Scope: the resumed `t_ef0c50ef` worker. Approved architecture groundwork was
independently reviewed READY by `gpt-6.1-sol/openai-codex`,
`deleg_eba11ebc/task-0`, against staged SHA-256
`b8d94b3c01e0d189a9a95accc1ed740cd0ec929c5b2fed2b9982d5ba17631415`.
Commit `fb7851a9cea07db64b2ee5826776cb5636141876` passed the architecture hook,
was pushed to `origin/update/V.1.2.0`, and `git ls-remote origin
refs/heads/update/V.1.2.0` read back the same SHA. This is a documentation
commit, not a completed spawn-clearance feature.

An uncommitted implementation attempted the approved SpawnUtil and
NaturalSpawner placement adapters. SpawnSizing delegated to the current
EntitySizing owner, refreshed fresh-candidate dimensions and checked actual
collision. SpawnUtil's existing failure branch correctly discarded/retried
instead of reporting a false successful Optional. NaturalSpawner checks vanilla
obstruction before finalization, but finalization can change age, dimensions
and attributes. The attempted adapter moved that one finalization call before
the acceptance branch and retained its returned group reference only on success.
That last design is unsafe: finalization is not a transaction.

Commands/results from the repository (logs below are under the runtime root):

- `./gradlew runGameTest --console=plain`, `evidence/spawn-golem-red.log`:
  **BUILD FAILED**, **24 tests, 1 failure**, enlarged village golem incorrectly
  accepted under the three-block roof. The corresponding
  `spawn-golem-green.log` passed **all 24** after the utility adapter.
- The retry test actually exposed a second candidate after the first failed:
  `run/gameTest/sizing-evidence/spawn-retries.txt` records `candidates=2`,
  registered success and height `3.9150002002716064`. The adequate/short golem
  measurements are in `village-golem.txt` in that same evidence directory.
- The natural-loop tracer in `spawn-natural-fixture.log` failed the actual
  finalized-scaled-collision assertion. The corrected loaded-fixture run,
  `spawn-natural-passed.log`, reported **BUILD SUCCESSFUL**, **26 tests passed**.
  It called real `NaturalSpawner.spawnCategoryForPosition` in a mushroom biome
  with a real GameTest server player: **723 short-space candidate predicates,
  0 short-space successes, 160 adequate-space successes**. Actual measurements
  are in `sizing-evidence/natural-ticking.txt`. These results did not expose the
  finalization side effects and do not establish the complete feature.
- Harness failures are retained, not presented as production red cycles:
  `spawn-natural-red.log` failed compilation on protected `Level.random`;
  `spawn-natural-red-runtime.log` reached no instance candidates before fixture
  lighting/loading was ready; `spawn-natural-green.log` hit registration timing;
  `spawn-natural-loaded.log` failed fixture tracking and an existing lifecycle
  assertion; `spawn-natural-loaded-2.log` crashed on a redundant fillbiome command
  (`No biome entries were changed`). The corrected fixture waits for actual
  entity loading/tracking and lighting; the passing run above is the evidence.
- Independent advisory source/bytecode review by
  `gpt-6.1-sol/openai-codex`, `deleg_02168e00/task-0`, returned **BLOCKED**.
  AgeableMob finalization mutates the passed group in place. Zombie finalization
  can flag/mount an existing chicken or register a new jockey chicken before
  clearance. Discarding the rejected mob does not roll those effects back.
  The moved direct call also bypasses wrappers on vanilla's original call site.
  No READY feature receipt was recorded.
- Added executable probes of the helper actually merged into transformed
  NaturalSpawner, then ran the same GameTest command:
  `evidence/spawn-finalization-blocker.log` **BUILD FAILED**, **28 tests,
  2 failures, 0 errors**. Both failures are the new transaction assertions.
  A rejected mooshroom changed an existing group's size **1 -> 2** and was
  discarded, with actual final height `2.0299999713897705`. A rejected baby zombie
  with a valid custom floor/hostileFloor `1.2` marked an **existing** chicken as
  a jockey on attempt index **15**; the chicken remained alive. The probe found
  **0 new orphan chickens** in that run: their possibility is bytecode evidence,
  not an observed orphan claim. These two probes call the real transformed
  private helper reflectively with real mobs/group state; they are not complete
  natural-loop end-to-end tests. The earlier ticking test exercises that loop.

Reproducer sources, tracked diff and failing XML are archived under
`../../runtime/realsize-mod/evidence/spawn-finalization-attempt/` and in
`spawn-finalization-attempt.zip` (SHA-256
`308df5b3fcaa75def57ba6b973eacf798f7b2c17eb5dd028517ab2880ef77367`). The archive
is based on `fb7851a`; its `src/` files reproduce the attempted source/wiring.
Actual measurements are `run/gameTest/sizing-evidence/finalization-group-blocker.txt`
and `finalization-jockey-blocker.txt`. Fresh local `javap -p -c` inspection is in
`evidence/spawn-ageable-finalization.txt` and `spawn-zombie-finalization.txt`.
The independent review transcripts are
`/home/p-h-r-e-d/phred2.0/home/cache/delegation/live/deleg_eba11ebc/task-0.log`
and `deleg_02168e00/task-0.log` in the same delegation directory.

All attempted production/test sources and metadata wiring were removed after
archiving; no unsafe feature source was committed or pushed. Restored
`./gradlew clean build --console=plain` reported **BUILD SUCCESSFUL**, **13
actionable tasks: 11 executed, 2 up-to-date**, **21 JUnit tests and 23 Minecraft
tests, 0 failures/errors/skipped** (`evidence/spawn-finalization-restored-build.log`).
`python3 scripts/server-smoke.py` passed dedicated startup, save/stop/exit **0**
and class isolation (`evidence/spawn-finalization-restored-smoke.log`). The
restored artifact SHA-256 is
`bf69a595be0705b1d926ccbbc23378c31581123cd0d16a9c6669d317fa291712`; direct JAR
inspection finds none of the attempted spawn classes/config/tests. An accessible
`/proc` cwd/fd scan found **0 runtime process references** after shutdown.

The approved narrow mixin exception remains valid; it does not by itself make
speculative finalization safe. A verified design/approval decision is needed for
finalized natural candidates: preserving the final-dimensions guarantee cannot
silently permit rejected candidates to mutate group or existing-world state.
No general transaction/rollback layer, species-size model, broader server hook,
or weakened acceptance was invented here. RS-08 and its dependent client/final
validation cards remain blocked. Chunk-generation behavior, explicit summon and
reload non-relocation acceptance, connected clients, single-player, profiling,
uninstall, CI and release gates are still unperformed. This new blocker section
is an uncommitted evidence update, not an independently approved feature commit.

### Resume verification — 2026-10-15 15:45 UTC

The next run re-read the card and approval: no new finalization design or scope
decision supersedes the blocker above. No production source was changed.
`git branch --show-current` reports `update/V.1.2.0`; HEAD and a fresh
`git ls-remote origin refs/heads/update/V.1.2.0` both identify
`fb7851a9cea07db64b2ee5826776cb5636141876`. Only FIXES.md is modified.

Fresh `./gradlew clean build --console=plain` failed: 23 Minecraft tests,
one failure in `registeredLifecycleLoadsAndReconcilesNaturalGrowth` at
`Vanilla attribute updates must refresh actual collision dimensions after
growth on tick 10`. Log: `evidence/spawn-resume-verification-build.log` under
the runtime root. An unchanged rerun of the same command passed: **BUILD
SUCCESSFUL**, 13 actionable tasks (11 executed, 2 up-to-date); parsed XML has
**23 Minecraft and 21 JUnit tests, zero failures/errors/skipped**. Retry log:
`evidence/spawn-resume-verification-retry-build.log`. The first failure remains
an unresolved intermittent groundwork failure, not a passing test or a proved
root cause. No test or acceptance criterion was weakened to obtain the retry.

Re-read archived XML confirms **28 tests, 2 failures**, specifically the
rejected-group and rejected-zombie assertions. Recomputed archive SHA-256
matches `308df5b3fcaa75def57ba6b973eacf798f7b2c17eb5dd028517ab2880ef77367`.
Saved measurements still show group size `1 -> 2` and the surviving existing
chicken marked as a jockey; these are archived observations, not a fresh run
of the removed unsafe implementation. Accessible `/proc` cwd/fd scan found no
runtime references after verification. `git diff --check` passed before this
evidence append. No feature commit, push, new review receipt, or next-card work
was performed. Elijah's finalization design/guarantee decision is still required;
the card must remain blocked rather than release its dependent.

## Bounded pre-finalization alternatives — 2026-10-15

Card `t_fa42570e` supersedes the preceding request for an unspecified design
decision. The narrow placement exception remains approved. This run investigated
new executable alternatives rather than repeating the unsafe early-finalization
implementation. **The feature is still not implemented.** The issue is a missing
authoritative dimension bound or separable finalization seam, not missing text
from Elijah's approval.

Verified local branch `update/V.1.2.0`, HEAD and fresh remote read-back both
`fb7851a9cea07db64b2ee5826776cb5636141876`. The architecture groundwork exists.
Only the preceding 132-line FIXES.md evidence update was dirty on arrival; it is
preserved. The project routing registry has no realsize entry; no other lane or
infrastructure configuration was changed. Java remains 25, Minecraft 26.3,
artifact version 1.2.0.

Two candidate designs were tested without changing production source:

1. Reconcile through the existing EntitySizing owner and check the fresh
   candidate's actual dimensions before vanilla finalization.
2. Check an envelope of EntityType dimensions times the configured cap before
   finalization, including a separate control accounting for an already-present,
   measured foreign total scale multiplier of 1.2.

The development-only `SpawnPreflightSpike` used the real Fabric GameTest server,
actual transformed Slime finalization, resolved settings and the existing owner.
It built a two-block-clearance room, checked each candidate before finalization,
executed the real finalizer once, reconciled/refreshed dimensions, then measured
actual world collision. It never registered the candidates. Each probe used 32
world-random seeds (43000 through 43031), with an existing AgeableMob group of
size 1. These are local finalization-boundary probes, **not end-to-end natural
placement/retry acceptance** or a test of foreign modifiers added by finalizers.

`./gradlew runGameTest --console=plain` exited **1**, **BUILD FAILED** (the test
server exited 3). Parsed XML: **26 Minecraft tests, exactly 3 failures, 0 errors**;
all failures are the three new probe assertions. Each probe found **11 false-clear
results out of 32**. Seed 43000 produced size 4:

- Current candidate: pre-height/envelope `0.7540000081062317`, final height
  `3.0160000324249268`; pre-check clear, finalized collision.
- Type/cap envelope: the same insufficient `0.7540000081062317` envelope and
  `3.0160000324249268` final height; pre-check clear, finalized collision.
- Foreign-inclusive type/cap envelope: envelope `0.9047999978065491`, final height
  `3.6191999912261963`; pre-check clear, finalized collision.

The group changed **1 -> 2** in all recorded rows. Fresh `javap -p -c -classpath
<pinned-common-jar> <class>` inspection covers SpawnUtil, NaturalSpawner,
EntityType, AgeableMob, Slime, MagmaCube, AbstractCubeMob and Zombie. In
AbstractCubeMob, finalization calls AgeableMob (including the shared-group
increment) before `setSpawnSize`; `getDefaultDimensions` multiplies the type
dimensions by that intrinsic size. RealSize's age-aware multiplier/cap is not a
bound on intrinsic variants. Reading current attributes or type dimensions does
not discover this final size. Applying another RealSize multiplier is not a fix.

Independent read-only review by Hermes subagent `gpt-6.1-sol/openai-codex`,
`deleg_9f672ebb/task-0`, verified the source, XML, measurements and fresh bytecode.
It confirmed both alternatives fail and found no demonstrated complete solution
inside the placement-only boundary. It explicitly did **not** prove that every
possible conservative envelope is impossible. No READY feature receipt was issued.

Other placement-only choices do not repair the demonstrated effects:

- Checking after vanilla finalization discovers dimensions, but cannot prevent
  the already-executed group or jockey effects. Suppressing assignment of returned
  group data/counters does not undo an in-place mutation.
- Moving/reusing a finalized candidate across retries leaves its first-location
  effects behind. Difficulty, group and jockey choices belong to that location;
  retry exhaustion still leaves effects. Re-finalizing also breaks the once-only
  contract. No rollback layer was introduced.
- Rejecting every uncertain type would suppress legitimate adequate-space
  spawns, not complete the feature. A species-maxima table would be the prohibited
  second model. Current type/cap bounds are now executably disproved.
- SpawnUtil obtains an already-finalized candidate from EntityType.create before
  its instance placement checks. Its golem fix is feasible in isolation, but
  shipping that partial fix would not solve NaturalSpawner or the all-candidate
  state guarantee for arbitrary SpawnUtil types.

**Concrete scope needed before implementation can continue:** authorize a
candidate preparation/commit seam in entity finalization, beyond the currently
approved SpawnUtil/NaturalSpawner adapters. Known required sites are
AgeableMob's shared-group increment, AbstractCubeMob's size selection after that
increment, and Zombie's existing-chicken flag/mount plus new-chicken registration.
The extension must let intrinsic final dimensions be resolved before externally
visible effects, retain the original invocation chain and once-only random
choices, and commit group/world effects only after clearance succeeds. These are
known minimum sites, **not an exhaustively audited sufficient patch list**;
remaining finalizers and foreign wrappers need auditing before promising coverage.
No broader mixin has been added or inferred from the placement approval.

An alternative would require an authoritative side-effect-free envelope API from
entity/finalization owners, with explicit intrinsic-variant/age/foreign-change and
positioning coverage. The pinned APIs inspected do not supply that bound. This
is not permission to invent a RealSize species table or weaken the guarantee.

Evidence lives under `../../runtime/realsize-mod/evidence/spawn-safe-preflight/`:
archived `SpawnPreflightSpike.java`, temporary `fabric.mod.json`, `probes.log`,
`probes-results.xml`, three `safe-preflight-*.txt` measurement files, and fresh
`*-bytecode.txt`. The independent review transcript is
`/home/p-h-r-e-d/phred2.0/home/cache/delegation/live/deleg_9f672ebb/task-0.log`.
`../spawn-safe-preflight.zip` archives this evidence plus restoration reports;
SHA-256 `1976023237ea9be926f242c0c3d09b0b595fa9ed65f9bf6c8811ff7269e2e63a`.

The throwaway class and metadata wiring were removed after archiving.
`./gradlew clean build --console=plain` then exited **0**, **BUILD SUCCESSFUL in
18s**, 13 actionable tasks (11 executed, 2 up-to-date). Parsed testcase elements:
**23 Minecraft tests, 0 failures/errors; 21 JUnit tests, 0 failures/errors/skipped**.
The existing natural-growth lifecycle test passed on this run and in the spike
run. The earlier intermittent failure remains unresolved; these passes are **not**
a fix or acceptance-by-retry. No unchanged retry was needed this run.
`python3 scripts/server-smoke.py` exited **0** and reported dedicated startup,
Done, save/stop/exit 0, and no RealSize/Minecraft client class loads. Logs are
`restored-build.log`, `restored-results.xml`, `restored-smoke.log` in the new
evidence directory. Restored JAR SHA-256 is
`bf69a595be0705b1d926ccbbc23378c31581123cd0d16a9c6669d317fa291712`.
Accessible `/proc` cwd/fd scan found **0 runtime process references** after smoke.

Only FIXES.md remains modified. No source commit/push, PR, release, production
change, tracking hook, client-culling implementation or downstream work occurred.
RS-08 remains open; all required feature acceptance gates not covered by these
counterexamples remain unperformed. The dependent client card stays gated until
the whole spawn feature is genuinely complete, reviewed, committed and pushed.

## Preparation-boundary progress and review-tool blocker — 2026-10-17

Card `t_fa42570e` resumed under the authoritative parent scope extension. This
run did not ask Elijah for another engineering permission. Necessary candidate
preparation/finalization hooks are approved under his delegated authority;
correctness, Warden and independent review remain required.

Fresh `./gradlew genSources --console=plain` exited **0**. The pinned Minecraft
26.3 common sources expose **59 entity finalizeSpawn declarations**. Inspection
also found fish-school leader updates, Drowned nautilus registration, Husk camel
and parched registration, and mount/dismount events beyond the earlier group/
chicken findings. Advisory independent design review `deleg_81d493e4/task-0`
identified transitive mount/abort effects, fresh vehicles/sibling ownership,
projection of existing-chicken attachment, and the need to check after the full
composed finalization expression rather than inside one wrapper.

README and ARCHITECTURE now define this bounded preparation/commit boundary.
Exact staged architecture diff SHA-256
`e3a02dfd4addd5bd074858e9b35f71e6573e2fcbb09c9328edeeaa3f4e9f3093` received
independent **READY, documentation only** from `gpt-6.1-sol/openai-codex`,
`deleg_a240956b/task-0`. The genuine receipt passed the architecture hook.
Commit `4759c7379ba2ce017af3cb6cfe7b9ff41b40d746` was pushed to
`origin/update/V.1.2.0`; `git ls-remote origin refs/heads/update/V.1.2.0` read
back that exact SHA. No production spawn feature is in that commit.

An uncommitted vertical implementation retained the original NaturalSpawner
finalization invocation and added a narrow post-expression rejection edge before
group assignment, counters, registration and callbacks. The returned group was
kept in a temporary local, not replaced with a sentinel visible to wrappers.
Scoped candidate preparation deferred explicit group/jockey/registration effects;
an ephemeral mount plan projected attachment positions without mutating existing
chickens. This implementation is **unfinished and not independently reviewed**.
It was archived and removed, not shipped as a partial feature.

Commands below ran from the repository. Logs are under
`../../runtime/realsize-mod/evidence/spawn-prepare-audit/`:

- `./gradlew runGameTest --console=plain`, `golem-red.log`: exit **1**, **24
  tests, one failure**, enlarged SpawnUtil golem accepted a three-block roof.
  `golem-green-target.log`: exit **0**, **24 tests, zero failures/errors**;
  the actual transformed path rejected the short roof, used exactly two candidates,
  then registered a final-height `3.9150002002716064` golem in four-block space.
  The first adapter run (`golem-green.log`) failed injection because its
  obstruction descriptor used ServerLevelAccessor instead of actual LevelReader;
  fresh javap inspection corrected the target. This is a harness/injection error,
  not a passing behavioral cycle.
- `natural-red-report.log`: exit **1**, **25 tests, one failure**, actual ticking
  NaturalSpawner accepted a finalized-scaled collision. An earlier harness
  (`natural-red.log`) wrapped the GameTest assertion and crashed the server;
  the corrected harness preserves the assertion type and produces failing XML.
  The initial plugin (`natural-edge.log`) failed closed on Minecraft's IFNE/GOTO
  guard shape; accepting that verified shape restored transformation. Natural
  clearance then passed in `natural-edge-guard.log`, but that run exposed the
  lifecycle failure described below.
- `group-red-target.log`: exit **1**, **25 tests, one failure**, rejected actual
  natural candidates incremented their groups. A development call-site wrapper
  observed real finalization/group objects. `group-green.log`: exit **0**, **25
  tests, zero failures/errors**, after deferred shared-group increments. The
  earlier `group-red.log` failed the test probe injection count; narrowing its
  selector to the exact ticking overload fixed the probe, not production behavior.
- `jockey-red.log`: exit **1**, **26 tests, one failure**, the actual SpawnUtil
  path changed the existing chicken at world-random seed **12280**. With scoped
  preparation, `jockey-prepare.log` exited **0**, **26 tests, zero failures/errors**.
  The new test exercised **4,000 seeds (12000 through 15999)** with a valid custom
  floor/hostileFloor `1.2` and a two-block roof. All returned empty; existing
  chicken flag, passengers, position and lifetime remained unchanged; a real
  ENTITY_LOAD observer saw no orphan registrations. These checks do not establish
  accepted jockey equivalence, all companion families or complete natural-loop
  side-effect coverage.

The lifecycle flake was reproduced again, not accepted by retry:
`natural-edge-guard.log` has **25 tests, one failure** at the existing exact
collision-width assertion on server tick 10. Source inspection shows
LivingEntity consumes dirty attributes during its own tick, while the existing
owner reconciles at END_LEVEL_TICK; newly added entities also wait for asynchronous
chunk tracking. Ten server ticks do not establish an additional entity tick after
the adult modifier update. The archived test change waits for actual adult/scale
reconciliation, then for `chicken.tickCount` to advance once, and retains the
same exact collision assertion. Subsequent changed-code runs passed. This is a
causal fixture correction candidate, **not a proved complete flake fix**: it
still needs independent review and a deterministic queued-update regression.
The source restoration below also restores the original flaky fixture.

The implementation was not ready for its mandatory final review. A further
advisory review request was refused by `delegate_task`: **"Delegation budget for
this one-shot run is exhausted (2/2 subagents used; delegation.oneshot_max_children)."**
That is an actual tool-capacity blocker, not a new scope-approval request. No
self-review or invented READY receipt substituted for the required independent
feature review. Resume in a fresh worker with an available reviewer slot; reserve
that slot for the eventual exact-staged feature review rather than spending it
on additional preliminary design reviews.

Unfinished source, tracked patch, manifest, failing/passing logs and the latest
26-test green XML are archived in
`../../runtime/realsize-mod/evidence/spawn-prepare-unfinished.zip`, SHA-256
`3c2a29bbae79f04c3e3f1e1f2ed0200d3d088eeaab8c8bb669e32198c2bfbcea`.
The unpacked `spawn-prepare-audit/unfinished-source/` tree and
`unfinished-tracked.diff` are based on architecture commit `4759c73`.
They are engineering evidence, not an installable or accepted deliverable.

Known required work before any feature acceptance/commit: complete Drowned/Husk
registration and fish-school effects; verify shared-group reads inside composed
finalizers; accepted mount-plan/replay equivalence and event behavior; actual
chunk-generation retries; both inner/outer foreign dimension wrappers; baby/
intrinsic variant/foreign dimensions; non-relocation of existing mobs, explicit
summons and reload; deterministic lifecycle regression; exact-staged independent
review, full build/smoke, coherent feature commit/push and remote SHA read-back.
The current passing slices do not waive any of these checks.

After archiving, all experimental source/metadata edits were restored to the
reviewed architecture commit; only the historical and this new FIXES.md evidence
remain dirty. `./gradlew clean build --console=plain` exited **0**; parsed reports
have **23 Minecraft and 21 JUnit tests, zero failures/errors/skipped**.
`python3 scripts/server-smoke.py` exited **0**. Restored JAR SHA-256 is
`bf69a595be0705b1d926ccbbc23378c31581123cd0d16a9c6669d317fa291712`;
direct JAR inspection found no experimental spawn owners/plugin/mixin config or
GameTests. Accessible `/proc` cwd/fd inspection found **zero runtime references**.
These restored passes are not spawn-feature acceptance or a lifecycle-flake fix.
No feature commit, main change, PR, release, production mutation, public metadata
deployment or downstream implementation occurred. Child `t_29eb3bd3` was read
and remains dependency-gated. RS-08 is open; this card must not complete.

## Resumed preparation tests and fail-closed commit blocker — 2026-10-17

Card `t_fa42570e` resumed from verified architecture commit
`4759c7379ba2ce017af3cb6cfe7b9ff41b40d746`. Fresh remote read-back matched.
The preceding archive checksum matched
`3c2a29bbae79f04c3e3f1e1f2ed0200d3d088eeaab8c8bb669e32198c2bfbcea`.
All prior FIXES evidence was retained. No new engineering permission was requested.

The restored experimental implementation gained explicit pending ageable-group
reads, deferred fish-school counter effects, deferred Drowned/Husk companion
registrations, and projected membership reads used by vanilla mount admission.
This is **unfinished, unreviewed feature source**, not an accepted implementation.
The original finalization call sites remain in place; no early-finalization or
rollback approach was restored.

Commands were run from the checkout. Logs below are under
`../../runtime/realsize-mod/evidence/spawn-resume/`:

- `./gradlew runGameTest --console=plain`, `effects-red.log`: exit **1**,
  **29 tests, 3 failures, 0 errors**. An outer finalizer observed a missing pending
  group increment; a rejected fish incremented an existing leader; 4,000 direct
  transformed Drowned/Husk finalization attempts (2,000 per type) produced **460
  companion registrations** before abort. `effects-red.xml` preserves that run.
  After the new effects/projection hooks, `effects-green.log`: exit **0**,
  **29 tests, zero failures/errors**. These detailed boundary tests are not
  substitutes for the actual placement-path tests.
- Same command, `mount-pairs.log`: exit **1**, **30 tests, 2 failures**. The
  lifecycle test's separate fixed two-tick wait also failed; it now waits for
  actual reconciliation without weakening the category assertion. The mount
  comparison initially used different random companion constructor yaws.
  `mount-measure.log` retained the measured attachment-position difference at
  Husk seed 60010. Both controls now use the same explicit yaw before comparing
  vanilla positioning with the prepared gate. This fixture correction is not
  claimed as a production mount fix.
- `paired-chunkgen.log` and `chunkgen-measure.log` failed the initial chunkgen
  fixture (no demonstrated candidate/finished entity tracking respectively).
  The corrected fixture uses a real mock server player, waits for entity loading
  and lighting, and reuses one seeded generation RNG rather than correlated
  first draws from adjacent seeds. These are harness corrections, not production
  red cycles. `chunkgen-player.log`: exit **0**, **31 tests, zero failures/errors**.
- Latest exact archived-source check, `checkpoint.log`: exit **0**, **31 tests,
  zero failures/errors**, confirmed by parsed `checkpoint.xml`. Actual
  `NaturalSpawner.spawnMobsForChunkGeneration` ran with two independently
  prioritized call-site wrappers (1100/900). Both executed once per finalized
  candidate. The outer wrapper introduced real collision after finalization;
  **90 rejected finalized candidates, zero registrations**, unchanged group
  counts and discarded candidates were observed. Open-space phase: **38
  finalized candidates and 38 registrations**, both foreign scale modifiers
  preserved, real final hitboxes clear. Distinct candidate coordinates verify
  the original retry coordinate continuation. See `chunkgen-coverage.txt`.
- The paired mount test used 1,000 seeds per Husk/Drowned/Spider/Strider, comparing
  actual vanilla finalization outside preparation with the prepared chain.
  Mounted cases observed were **102 Husk, 31 Drowned, 9 Spider, 128 Strider**;
  see `mount-coverage.txt`. Tests compared age, tree topology, attachment positions,
  registration order and accepted mount-event counts, and checked abort emits no
  mount/dismount events and removes fresh vehicles/siblings. The first actual
  accepted riding-position update agreed with clearance. These are bounded
  transformed finalization comparisons with normalized yaw, not all-mod
  compatibility or complete natural-loop jockey acceptance. Accepted dismount
  event equivalence and the existing-chicken accepted case still need coverage.

The necessary exact architecture-tree inventory received independent
**READY, documentation only** from `gpt-6.1-sol/openai-codex`,
`deleg_8877afc8/task-0`; staged diff SHA-256
`e4b753918eed7637778f537f789890d07b330c46b2fced41426f6ecd6dc770f9`.
The reviewer inspected the actual staged diff; the genuine receipt was recorded.
`git commit -m 'Declare spawn preparation adapter layout'` then exited **1**:
architecture conformance passed, but the mandatory scanner reported:
`secret-scan: BLOCKED — required credential scan could not run: secret store not found: /home/p-h-r-e-d/.secrets/store.json`.
The physical pre-commit hook failed closed. No commit was created and no scanner,
hook or credential-store configuration was bypassed or modified. Further source
work stopped at this actual tool prerequisite, not a new scope-approval question.
Restore the required store through its authorized owner, or repair the canonical
scanner's store resolution in its own reviewed infrastructure work, before retrying.

New archive: `../../runtime/realsize-mod/evidence/spawn-resume-unfinished.zip`,
SHA-256 `0c06f090c2ab6a75571a44d23641f64359e9d7b1365771511dc3a02eda6290f5`.
Its **44 entries** include **28 source/architecture files**, tracked and reviewed
architecture patches, a per-file SHA manifest, logs, real coverage measurements,
and exact latest XML. ZIP integrity and every archived source byte were checked
against the checkout before restoration. Unpacked source is
`spawn-resume/unfinished-source/`, based on `4759c73`; the original prior archive
is unchanged. The architecture inventory patch is reviewed but **not committed**;
it needs a fresh hash check and valid receipt after restoring it. It must commit
before the eventual feature commit. Architecture READY is not feature READY.

Remaining feature work includes finalizer/wrapper audit and independent exact
feature review; complete variant/baby/foreign-dimension and accepted existing-
chicken coverage; deterministic queued-attribute lifecycle regression (the causal
wait corrections alone are not that regression); explicit summons/existing-mob/
reload non-relocation acceptance; full candidate build/artifact smoke; feature
commit/push and exact remote read-back. No `SpawnDimensionGameTests` stub was
created: that declared path is intended for these remaining acceptance tests.
The current 31 passing slices do not waive any missing criterion.

After archiving, all experimental source and the uncommitted architecture patch
were restored to `4759c73`; only FIXES.md remains dirty. Restored
`./gradlew clean build --console=plain`, `restored-build.log`: exit **0**,
**23 Minecraft and 21 JUnit tests, zero failures/errors/skipped**. The original
lifecycle fixture is restored too; its passing restoration run is not its fix.
`python3 scripts/server-smoke.py`, `restored-smoke.log`: exit **0**, dedicated
startup, save/stop/exit 0 and client isolation passed. Restored JAR SHA-256:
`bf69a595be0705b1d926ccbbc23378c31581123cd0d16a9c6669d317fa291712`.
JAR inspection found no experimental spawn/config/test entries. Accessible
`/proc` cwd/fd scan found **zero runtime process references**. Local HEAD and
fresh remote SHA remain `4759c7379ba2ce017af3cb6cfe7b9ff41b40d746`.
No feature review, commit/push, main change, PR, release, production mutation,
public metadata deployment or downstream implementation occurred. Child
`t_29eb3bd3` was inspected and remains dependency-gated. RS-08 stays open.

## Spawn feature candidate and resumed acceptance — 2026-10-17

The live base and fresh remote were exactly `4759c7379ba2ce017af3cb6cfe7b9ff41b40d746`.
The newest 44-entry archive matched SHA-256
`0c06f090c2ab6a75571a44d23641f64359e9d7b1365771511dc3a02eda6290f5`;
all 28 manifest entries matched before restoring the 27 source/resource files.
Prior FIXES bytes were preserved. The obsolete scanner failure above is historical:
the installed physical hook now runs architecture conformance, not that removed
scanner. No credentials, hook, scanner or Warden configuration changed.

The exact architecture inventory diff again hashed to
`e4b753918eed7637778f537f789890d07b330c46b2fced41426f6ecd6dc770f9`.
Independent `gpt-6.1-sol` reviewer `deleg_6e6a4dfe/task-0` returned READY for that
documentation-only staged diff. The physical hook passed on the real nonempty
commit `bb22df16af15857a0b45618825fbba72c66425b4`, before source restoration.
This architecture review is not feature approval.

The preparation adapters preserve the original finalization invocations and
their composed wrappers. The NaturalSpawner plugin adds only the rejection
continuation before group assignment/counters/registration, using a temporary
group local. SpawnUtil contains effects across full EntityType creation and its
original admission checks. EntitySizing remains the sole sizing mutation owner.
Candidate scope contains group/school counts, fresh companions, projected mounts,
existing-chicken state and explicitly queued registration/events/sounds. No
general rollback, second species-sizing model, tracking or networking was added.

The resumed audit extracted and inspected 59 entity finalizer bodies from the
pinned 26.3 sources; `evidence/spawn-resume/finalizer-audit-source.txt` preserves
them. Externally visible families are ageable group counts, schooling-fish leader
counts, zombie chicken state/registration, Husk/Drowned registrations, Spider/
Strider/ZombieHorse attachment helpers, and CopperGolem/Warden spawn sounds.
Candidate-local variants, age, equipment, attributes and brain memories execute
once. Arbitrary foreign finalizers with additional external writes still require
cooperative integration; the README/architecture explicitly exclude that promise.

New transformed evidence under `../../runtime/realsize-mod/evidence/spawn-resume/`:

- `dismount-red.log/.xml`: 32 tests, two failures: explicit removeVehicle left
  projected membership stale, and the chunkgen readiness fixture timed out.
  The explicit dismount/cooldown projection seam then passed in
  `dismount-green.log` (32 tests, zero failures). Abort emits no mount/dismount
  events; accepted replay emits two mounts and one dismount and respects the
  vanilla remount cooldown. This is a production red-to-green cycle.
- The preexisting growth fixture now waits for actual reconciliation and one
  additional entity tick, retaining exact bounding-box assertions. The new
  deterministic test runs real chicken ticks and the registered end-level event:
  reconciliation queues SCALE dirt while bbox is stale; the next entity tick
  consumes it and refreshes the exact adult bbox. This directly demonstrates why
  a fixed server-tick delay was insufficient, without retrying unchanged tests.
- Actual SpawnUtil tests cover 128 seeded chicken/slime candidates, real baby
  and intrinsic slime sizes, a late foreign scale modifier, and a transformed
  foreign dimension-return modifier independent of SCALE. Accepted results use
  final clear hitboxes and vanilla coordinates; rejected candidates are removed
  before registration. Existing golem retry, actual ticking natural-loop group
  rejection, chunkgen retry and 1100/900 composed-wrapper tests remain active.
- Accepted existing-chicken SpawnUtil is paired with actual unprepared vanilla
  finalization/registration, preserving existing chicken bbox/position and
  accepted rider position/event/registration sequence. Seed 12280 is included.
  The fixture now awaits real dark-room lighting before vanilla walk-target
  admission; earlier open/instant-dark fixtures failed admission independently
  of preparation and were not claimed as production bugs.
- Real summon command, existing colliding golem reload, and real save/load tests
  verify that placement scope does not veto, delete or add relocation. Vanilla
  refreshDimensions itself nudges the roof-colliding golem upward by 0.000001;
  the test compares exact positions against equally scaled vanilla controls,
  rather than suppressing that behavior or claiming literally zero movement.
  A first command fixture used scientific-notation coordinates rejected by
  Brigadier; fixed-format decimal coordinates correct the fixture.
- The fast-ticking GameTestServer burned the old 400-tick chunkgen entity-I/O
  readiness timeout in under 300 ms. A bounded 1 ms backoff only while storage
  remains unready gives actual I/O time; readiness and collision assertions stay
  required. This is a fixture correction, not a placement production fix.
- The full build first exposed the scaffold-era blanket no-mixins package
  assertion (21 JUnit, one failure). It now verifies the actual packaged common
  spawn config, required injections and adapter/plugin classes, while forbidding
  unimplemented client mixins and retaining metadata/client/test isolation checks.

`./gradlew clean build --console=plain` (`candidate-build.log`) exited 0:
36 Minecraft tests and 21 JUnit tests, zero failures/errors/skipped.
`python3 scripts/server-smoke.py` (`candidate-smoke.log`) exited 0, dedicated
startup/save/stop and client isolation passed. An additional actual-artifact
probe `python3 ../../runtime/realsize-mod/evidence/spawn-resume/artifact-smoke.py`
also exited 0: Fabric KnotServer loaded RealSize from the packaged JAR, not
build/classes, reached Done, saved/stopped/exited 0 with no development test mod
or Minecraft/RealSize client class loading. It used the pinned development game
and dependency classpath with source outputs removed, not a separately installed
production distribution. The exact tested JAR SHA-256 is
`9c2302827dbb04b7814c528cdcec8e72a54da096d50353e21b4124bb5268e6f3`.

Feature exact-staged independent review and final commit/push/read-back are still
pending at this candidate checkpoint. RS-08 and the dependent client gate are
not marked complete by these test results alone. No player-facing deployment,
production action, main/PR/merge/release or downstream feature was performed.

### Independent feature review and implicit-remount correction

`gpt-6.1-sol` reviewer `deleg_3ba67781/task-0` inspected exact staged feature
SHA-256 `c25fbdd74ed578532c3d509efaf5c7497365a9b8091b41aa075244054524a0e5`
and returned REQUIRED CHANGES, not READY. Its two transformed regressions found
the same concrete blocker: a direct forced remount and actual vanilla Husk
seed 58 could change rider position during commit, after clearance had passed.
The prior explicit-removeVehicle regression did not exercise that implicit path.

The permanent actual-Husk regression was added before the fix. Parent execution
`implicit-remount-red.log/.xml` reproduced exactly one failure among 37 tests:
the checked rider Y was 9.8125 and the committed Y was 8.0 (X/Z also changed).
Pinned Entity.startRiding invokes LivingEntity.stopRiding before a vehicle
switch; that method computes dismount positioning. Preparation had updated only
projected membership, leaving that positional effect until replay.

The bounded fix calls the candidate's original stopRiding during preparation.
Its candidate-local position/pose logic executes once, while removeVehicle
projects membership and queues the external removal before the new mount.
Replay therefore has no old vehicle to implicitly dismount after clearance.
There is no post-commit position reset, re-finalization or generic rollback.

`./gradlew clean build --console=plain` (`review-fix-build.log`) exited 0:
37 Minecraft tests and 21 JUnit tests, zero failures/errors/skipped. This includes
the permanent actual Husk seed-58 regression comparing both position and bbox
before/after commit and the first actual riding-position update. The actual-JAR
probe (`review-fix-artifact-smoke.log`) also exited 0, startup/save/stop/client
isolation passed; corrected JAR SHA-256 is
`5a80dfbffac6b1edd7988534582a9e01a4bac8210acc0201446e30335c426ee2`.
The feature still needs independent exact-staged re-review of this correction
before commit/push and downstream acceptance. The first feature review is not
being reported as READY. Whole-branch/client/release validation remains separate.
