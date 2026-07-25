# GitHub Releases

This repository is configured to publish release assets for SheepMerge live updates from `0x208u16/SheepFarmMinigamePlugin`.

## How to publish a release

Preferred path:

```bash
scripts/publish-release.sh 1.0.1
```

This script:
- updates `app/src/main/resources/plugin.yml`
- builds the plugin
- commits the version bump
- creates tag `v<version>`
- pushes the branch and tag

Manual path:

1. Update `app/src/main/resources/plugin.yml` with the new plugin version.
2. Commit and push the version change.
3. Create and push a tag in the form `vX.Y.Z`.
4. GitHub Actions will build the plugin and publish a GitHub Release with:
   - `SheepMerge-X.Y.Z.jar`
   - `live-update.yml`
   - generated changelog notes

## Live update manifest

The `live-update.yml` asset is consumed by the in-plugin live update checker.

Fields:
- `tagName`
- `dataSchemaVersion`
- `requiresBinarySwap`
- `liveSafeMigration`
- `binaryAssetName`
- `summary`
- `reloadConfiguration`

## Public access requirement

For the in-game live updater to pull release assets without authentication, the GitHub repository itself must be public in GitHub settings.

The required repository URL is:

`https://github.com/0x208u16/SheepFarmMinigamePlugin`
