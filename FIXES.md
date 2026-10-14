# RealSize v2 fixes and acceptance

Status: open implementation work. README and ARCHITECTURE describe the final
product in present tense at Elijah's request; this tracker describes reality.
The supplied audit concerns the removed legacy implementation. Its exact mob
visibility distances, allocation rates, and gameplay outcomes are reported
findings, not locally reproduced measurements. Do not mark them verified from
this document alone.

Current checkout: build-tool groundwork, no replacement production classes,
metadata, tests, or installable JAR. Existing groundwork uses one 26.3 project,
Java 25, non-remapping Loom, and no identity mapping JAR. No gameplay fixes are
implemented by this documentation update. The former FEATURES.md mentioned
in the audit is not present in this checkout; ARCHITECTURE is the active contract.

## Open fixes

- [ ] **RS-01 — Tiny-mob visibility.** Drop server tracking extensions; add the
  optional client unscaled-distance correction in the same JAR. Reproduce the
  bee disappearance around 12 blocks before fixing it. Compare vanilla and
  modded clients, toggle off/on, verify server tracking unchanged and dedicated
  server client-class isolation. Treat audit distances as approximate until tested.
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
- [ ] **RS-09 — Build and dev runtime.** Finish the single 26.3 artifact, root
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
and CI builds the tested single artifact. Current metadata and documentation
match that result. Any remaining exception is recorded here before release;
release approval is separate from committing documentation.

## State

2026-10-13: recorded the supplied legacy audit as open v2 fixes and replaced the
old architecture/product contract with the finished-state target. Documentation
only; implementation, gameplay verification, and external metadata edits remain
open. The build property remains 1.2.0; v2 is a rebuild name, not a silent bump.
