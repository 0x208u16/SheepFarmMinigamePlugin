#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/publish-release.sh <version> [--no-build] [--no-push]

Bumps app/src/main/resources/plugin.yml to the given version,
builds the plugin, commits the version change, creates tag v<version>,
and optionally pushes the commit and tag so GitHub Actions publishes a release.

Examples:
  scripts/publish-release.sh 1.0.1
  scripts/publish-release.sh 1.0.1 --no-push
EOF
}

if [[ $# -lt 1 ]]; then
  usage
  exit 1
fi

version=""
do_build=1
do_push=1

for arg in "$@"; do
  case "$arg" in
    --no-build)
      do_build=0
      ;;
    --no-push)
      do_push=0
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      if [[ -z "$version" ]]; then
        version="$arg"
      else
        echo "Unexpected argument: $arg" >&2
        usage
        exit 1
      fi
      ;;
  esac
done

if [[ -z "$version" ]]; then
  echo "Missing version argument." >&2
  usage
  exit 1
fi

if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][A-Za-z0-9]+)?$ ]]; then
  echo "Version must look like X.Y.Z or X.Y.Z-suffix" >&2
  exit 1
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

plugin_yml="app/src/main/resources/plugin.yml"
if [[ ! -f "$plugin_yml" ]]; then
  echo "Could not find $plugin_yml" >&2
  exit 1
fi

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Working tree is not clean. Commit or stash changes before publishing." >&2
  exit 1
fi

current_version="$(grep '^version:' "$plugin_yml" | awk '{print $2}')"
if [[ -z "$current_version" ]]; then
  echo "Could not read current version from $plugin_yml" >&2
  exit 1
fi

if [[ "$current_version" == "$version" ]]; then
  echo "Version is already $version" >&2
  exit 1
fi

if git rev-parse -q --verify "refs/tags/v$version" >/dev/null; then
  echo "Tag v$version already exists." >&2
  exit 1
fi

sed -i -E "s/^version: .*/version: $version/" "$plugin_yml"

echo "Updated version: $current_version -> $version"

if [[ $do_build -eq 1 ]]; then
  GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon :app:build
fi

git add "$plugin_yml"
git commit -m "Release v$version"
git tag "v$version"

branch="$(git branch --show-current)"
if [[ -z "$branch" ]]; then
  echo "Could not determine current branch." >&2
  exit 1
fi

if [[ $do_push -eq 1 ]]; then
  git push origin "$branch"
  git push origin "v$version"
  echo "Pushed commit and tag. GitHub Actions will publish the release assets."
else
  echo "Created commit and tag locally. Push when ready:"
  echo "  git push origin $branch"
  echo "  git push origin v$version"
fi
