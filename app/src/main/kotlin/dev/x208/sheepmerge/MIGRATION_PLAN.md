# Kotlin Migration Plan (Paper 1.20.x Compatible)

This plugin can be migrated to Kotlin incrementally while staying compatible with current Paper and Java 17 bytecode.

## Compatibility Rules

- Keep plugin entry class unchanged until final cutover: `dev.x208.sheepmerge.SheepMergePlugin`
- Keep package names and public APIs stable during migration.
- Compile both Java and Kotlin to JVM target 17.
- Preserve serialized config/data keys exactly (`scores.yml`, layout data, unlock maps).
- Keep Bukkit/Paper API usage semantics unchanged in behavior-critical paths.

## Migration Status

- Done:
  - Kotlin build enablement and runtime packaging in `app/build.gradle`
  - Utility conversion to Kotlin:
    - `InventoryDataUtils`
    - `SacrificeUnlockState`
    - `MenuItemFactory`
  - Command module layer converted to Kotlin:
    - `commands/SheepMergeCommandModule`
    - `commands/BaseRootCommandModule`
    - all root wrapper modules in `commands/RootCommandModules.kt`
  - Domain/listener conversion:
    - `SheepTier`
    - `SheepEggModule`
    - `SheepMergeWorldListener`
    - `SheepFarmGameListener`
    - `SheepFarmWorldProtectionListener`
    - `SheepFarmWorldCleanupListener`
  - bStats layer converted to Kotlin:
    - `bstats/Metrics`
- Remaining Java footprint:
  - Main blockers by size:
    - `SheepMergeManager.java`
    - `SheepFarmWorldCommand.java`

## Migration Phases (Updated)

1. Command Module Layer (in progress)
- Convert `commands/*` classes first (low-risk wrappers + interface/core module base).
- Preserve class names and constructor signatures for Java caller compatibility.

2. Domain + Mid-size Components
- Convert:
  - `SheepTier`
  - `SheepEggModule`
  - `SheepMergeConfiguration`

3. Listener Layer
- Convert listeners one by one while preserving event priorities and cancellation behavior:
  - `SheepMergeWorldListener`
  - `SheepFarmGameListener` (done)
  - `SheepFarmWorldProtectionListener`
  - `SheepFarmWorldCleanupListener`

4. Command Engine
- Convert `SheepFarmWorldCommand` after module conversion is complete.

5. Core Manager Split + Convert
- Split `SheepMergeManager` into Kotlin feature files before full conversion:
  - economy, automation, achievements, world lifecycle, menus/UI, persistence.
- Keep static API compatibility by exposing `@JvmStatic` members where Java callers remain.

6. Plugin Entry + Final Cutover
- Convert `SheepMergePlugin` last.
- Keep `plugin.yml` main class stable until final class cutover is validated.

## Optimization Targets During Rewrite

- Break `SheepMergeManager` into cohesive Kotlin files/objects by feature area:
  - economy, achievements, automation, world lifecycle, UI/menu.
- Replace repeated map boilerplate with helper extensions and typed state wrappers.
- Remove duplicate scoreboard/menu refresh paths.
- Use immutable snapshots for menu item generation to reduce accidental state coupling.
- Keep async/sync boundaries explicit with named scheduler wrappers.

## Validation Checklist per Phase

- `./gradlew :app:compileJava`
- `./gradlew :app:build`
- Run plugin in test server and smoke test:
  - join/leave
  - `/sheepmerge` core flow
  - sheep spawn/shear/merge
  - quick access actions
  - automation toggles
  - data save/load across restart

## Execution Notes

- Every migration batch should end with a compile check.
- Any file with gameplay-state persistence must preserve existing YAML keys exactly.
- Avoid behavior changes during migration; optimization changes are isolated to post-parity passes.

## Notes

A full one-shot rewrite is not recommended for this codebase size. Incremental conversion preserves production stability and allows optimization with measurable regression checks.
