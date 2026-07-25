# GitHub Releases

This repository is configured to publish release assets for SheepMerge live updates from `0x208u16/SheepFarmMinigamePlugin`.

## How to publish a release

Preferred path:

```bash
export GITHUB_TOKEN=<github_token_with_repo_access>
scripts/publish-release.sh 1.0.1
```

If you use GitHub CLI auth locally, the script will also auto-detect `gh auth token`.

This script:
- uses the local SSH key pair `sheep_merge_key` / `sheep_merge_key.pub` for GitHub pushes
- uses authenticated GitHub API polling when `GITHUB_TOKEN`/`GH_TOKEN`/`gh auth token` is available
- updates `app/src/main/resources/plugin.yml`
- builds the plugin
- commits the version bump
- creates tag `v<version>`
- pushes the tag first, waits for the GitHub Release to appear, then pushes the branch
- detects stale failed publish tags and cleans them up before retrying
- rolls back the local version-bump commit/tag if release publication fails

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

The local release keys are intentionally ignored by git and must never be committed.
