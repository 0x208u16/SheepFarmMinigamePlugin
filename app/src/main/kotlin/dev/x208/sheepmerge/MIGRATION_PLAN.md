# Kotlin Migration Plan (Paper 1.20.x Compatible)

This plugin can be migrated to Kotlin incrementally while staying compatible with current Paper and Java 17 bytecode.

## Compatibility Rules

- Keep plugin entry class unchanged until final cutover: `dev.x208.sheepmerge.SheepMergePlugin`
- Keep package names and public APIs stable during migration.
- Compile both Java and Kotlin to JVM target 17.
- Preserve serialized config/data keys exactly (`scores.yml`, layout data, unlock maps).
- Keep Bukkit/Paper API usage semantics unchanged in behavior-critical paths.

## Migration Phases

1. Build Enablement (done)
- Add Kotlin Gradle plugin and stdlib.
- Ensure Kotlin compiles to JVM 17.

2. Safe Utility Conversion
- Convert low-risk utility classes first:
  - `InventoryDataUtils`
  - `SacrificeUnlockState`
  - `MenuItemFactory`
- Preserve method signatures used by Java callers.

3. Domain Enums and Small Models
- Convert `SheepTier` and lightweight state holders.
- Keep static-like access with Kotlin `companion object` and `@JvmStatic` where needed.

4. Listener and Command Layer
- Convert listeners one by one:
  - `SheepMergeWorldListener`
  - `SheepFarmGameListener`
  - `SheepFarmWorldProtectionListener`
  - `SheepFarmWorldCleanupListener`
- Then convert `SheepFarmWorldCommand`.

5. Core Manager + Plugin
- Convert `SheepMergeManager` in slices by feature blocks.
- Convert `SheepMergePlugin` last.
- Update `plugin.yml` main class only after the Kotlin plugin class is final.

## Optimization Targets During Rewrite

- Break `SheepMergeManager` into cohesive Kotlin files/objects by feature area:
  - economy, achievements, automation, world lifecycle, UI/menu.
- Replace repeated map boilerplate with helper extensions and typed state wrappers.
- Remove duplicate scoreboard/menu refresh paths.
- Use immutable snapshots for menu item generation to reduce accidental state coupling.
- Keep async/sync boundaries explicit with named scheduler wrappers.

## Validation Checklist per Phase

- `./gradlew :app:compileJava`
- Run plugin in test server and smoke test:
  - join/leave
  - `/sheepmerge` core flow
  - sheep spawn/shear/merge
  - quick access actions
  - automation toggles
  - data save/load across restart

## Notes

A full one-shot rewrite is not recommended for this codebase size. Incremental conversion preserves production stability and allows optimization with measurable regression checks.
