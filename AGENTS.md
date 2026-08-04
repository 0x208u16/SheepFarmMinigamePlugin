# AGENTS

## Live Update Compatibility Rule

When changing gameplay state, persistence, release automation, or configuration, coding agents must verify that the change remains compatible with SheepMerge live updates.

Required checks before finishing a change:
- Verify whether the change affects persisted data in `scores.yml`, `farm-layout.yml`, or runtime state restored after startup.
- If persisted structure changes, update the live migration/schema handling in `SheepMergeManager`.
- If release asset names, version formats, or manifest fields change, verify compatibility with `LiveUpdateCoordinator`, `.github/workflows/release.yml`, `release/live-update.yml`, and `scripts/publish-release.sh`.
- If a change is not safe for in-place live migration, ensure it continues to require staged binary swap/restart rather than silent live apply.
- Run a build check after the change.

## Release Tag Rule

GitHub Releases are published from tags in the form `vX.Y.Z`, while the plugin runtime version is stored in `plugin.yml` as `X.Y.Z`.
Any code comparing release versions must normalize the optional `v` prefix.

## Java to Kotlin Migration Rule

Changes to code must be limited to code migration. Do not make changes irrelevant to the code migration.
