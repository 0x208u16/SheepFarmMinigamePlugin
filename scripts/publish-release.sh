#!/usr/bin/env bash
set -euo pipefail

KEY_PATH="/home/dev/Projects/Paper/Plugins/SheepMerge/sheep_merge_key"
PUB_KEY_PATH="/home/dev/Projects/Paper/Plugins/SheepMerge/sheep_merge_key.pub"
GRADLE_PROPERTIES="gradle.properties"
PLUGIN_YML="app/src/main/resources/plugin.yml"
RELEASE_POLL_RETRIES=30
RELEASE_POLL_SECONDS=10
publish_remote=""
pre_release_head=""
ssh_command=()

cleanup_on_failure() {
  local version="$1"
  local tag="v$version"

  if [[ -n "$publish_remote" ]] && GIT_SSH_COMMAND="${ssh_command[*]}" git ls-remote --tags "$publish_remote" "refs/tags/$tag" | grep -q "$tag"; then
    GIT_SSH_COMMAND="${ssh_command[*]}" git push "$publish_remote" ":refs/tags/$tag" >/dev/null 2>&1 || true
  fi

  if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
    git tag -d "$tag" >/dev/null 2>&1 || true
  fi

  if [[ -n "$pre_release_head" ]] && git rev-parse --verify HEAD^ >/dev/null 2>&1 && [[ "$(git rev-parse --short HEAD^)" == "$pre_release_head" ]]; then
    git reset --hard "$pre_release_head" >/dev/null 2>&1 || true
  fi
}

git_has_relevant_changes() {
  local status
  status="$(git status --porcelain -- . ':(exclude).gradle-home' ':(exclude).gradle' ':(exclude)build' ':(exclude)app/build')"
  [[ -n "$status" ]]
}

read_property() {
  local key="$1"
  grep -E "^${key}[[:space:]]*=" "$GRADLE_PROPERTIES" | sed -E 's/^[^=]+=[[:space:]]*//' | tail -n 1
}

normalize_repo_url() {
  local owner="$1"
  local repo="$2"
  echo "git@github.com:${owner}/${repo}.git"
}

release_exists() {
  local owner="$1"
  local repo="$2"
  local tag="$3"
  curl -fsS "https://api.github.com/repos/${owner}/${repo}/releases/tags/${tag}" >/dev/null 2>&1
}

verify_remote_access() {
  if ! GIT_SSH_COMMAND="${ssh_command[*]}" git ls-remote --heads "$publish_remote" >/dev/null 2>&1; then
    echo "Unable to access $publish_remote using $KEY_PATH" >&2
    echo "Check that the private key is authorized for the repository and that the repo exists." >&2
    exit 1
  fi
}

wait_for_release() {
  local owner="$1"
  local repo="$2"
  local tag="$3"

  for (( attempt=1; attempt<=RELEASE_POLL_RETRIES; attempt++ )); do
    if release_exists "$owner" "$repo" "$tag"; then
      echo "GitHub Release $tag is live."
      return 0
    fi
    echo "Waiting for GitHub Release $tag to appear (${attempt}/${RELEASE_POLL_RETRIES})..."
    sleep "$RELEASE_POLL_SECONDS"
  done
  return 1
}

cleanup_stale_failed_release() {
  local owner="$1"
  local repo="$2"
  local version="$3"
  local tag="v$version"

  if GIT_SSH_COMMAND="${ssh_command[*]}" git ls-remote --tags "$publish_remote" "refs/tags/$tag" | grep -q "$tag"; then
    if ! release_exists "$owner" "$repo" "$tag"; then
      echo "Found stale tag $tag without a published release. Cleaning it up first."
      GIT_SSH_COMMAND="${ssh_command[*]}" git push "$publish_remote" ":refs/tags/$tag"
    fi
  fi

  if git rev-parse -q --verify "refs/tags/$tag" >/dev/null && ! release_exists "$owner" "$repo" "$tag"; then
    git tag -d "$tag" >/dev/null 2>&1 || true
  fi
}

usage() {
  cat <<'EOF'
Usage: scripts/publish-release.sh <version> [--no-build] [--no-push]

Bumps app/src/main/resources/plugin.yml to the given version,
builds the plugin, commits the version change, creates tag v<version>,
pushes the branch and tag using the SheepMerge repository SSH key, waits for GitHub Releases
to publish successfully, and cleans up failed publish attempts.

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
resume_publish=0

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

if [[ ! -f "$PLUGIN_YML" ]]; then
  echo "Could not find $PLUGIN_YML" >&2
  exit 1
fi

if [[ ! -f "$KEY_PATH" || ! -f "$PUB_KEY_PATH" ]]; then
  echo "Missing publish key files. Expected: $KEY_PATH and $PUB_KEY_PATH" >&2
  exit 1
fi

chmod 600 "$KEY_PATH"

github_owner="$(read_property pluginOwner)"
github_repo="$(read_property githubRepoName)"
if [[ -z "$github_owner" || -z "$github_repo" ]]; then
  echo "Could not read pluginOwner/githubRepoName from $GRADLE_PROPERTIES" >&2
  exit 1
fi

publish_remote="$(normalize_repo_url "$github_owner" "$github_repo")"
ssh_command=(ssh -i "$KEY_PATH" -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new)

verify_remote_access

if git_has_relevant_changes; then
  echo "Working tree is not clean. Commit or stash changes before publishing." >&2
  exit 1
fi

current_version="$(grep '^version:' "$PLUGIN_YML" | awk '{print $2}')"
if [[ -z "$current_version" ]]; then
  echo "Could not read current version from $PLUGIN_YML" >&2
  exit 1
fi

if [[ "$current_version" == "$version" ]]; then
  if git rev-parse -q --verify "refs/tags/v$version" >/dev/null; then
    resume_publish=1
    echo "Version is already $version and local tag v$version exists. Resuming incomplete publish."
  else
    echo "Version is already $version" >&2
    exit 1
  fi
fi

pre_release_head="$(git rev-parse --short HEAD)"

cleanup_stale_failed_release "$github_owner" "$github_repo" "$version"

if [[ $resume_publish -eq 0 ]] && git rev-parse -q --verify "refs/tags/v$version" >/dev/null; then
  echo "Tag v$version already exists locally." >&2
  exit 1
fi

if [[ $resume_publish -eq 0 ]]; then
  sed -i -E "s/^version: .*/version: $version/" "$PLUGIN_YML"

  echo "Updated version: $current_version -> $version"

  if [[ $do_build -eq 1 ]]; then
    GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon :app:build
  fi

  git add "$PLUGIN_YML"
  git commit -m "Release v$version"
  git tag "v$version"
else
  if [[ $do_build -eq 1 ]]; then
    GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon :app:build
  fi
fi

branch="$(git branch --show-current)"
if [[ -z "$branch" ]]; then
  echo "Could not determine current branch." >&2
  exit 1
fi

if [[ $do_push -eq 1 ]]; then
  if ! GIT_SSH_COMMAND="${ssh_command[*]}" git push "$publish_remote" "$branch"; then
    cleanup_on_failure "$version"
    echo "Failed to push branch $branch. Cleaned up local tag/commit state." >&2
    exit 1
  fi

  if ! GIT_SSH_COMMAND="${ssh_command[*]}" git push "$publish_remote" "v$version"; then
    cleanup_on_failure "$version"
    echo "Failed to push tag v$version. Cleaned up local tag/commit state." >&2
    exit 1
  fi

  if ! wait_for_release "$github_owner" "$github_repo" "v$version"; then
    cleanup_on_failure "$version"
    echo "GitHub Release v$version did not publish successfully in time. Cleaned up failed publish tag and local state." >&2
    echo "Check the GitHub Actions workflow for release failures." >&2
    exit 1
  fi

  echo "Published release v$version and pushed branch commit successfully."
else
  echo "Created commit and tag locally. Push when ready:"
  echo "  GIT_SSH_COMMAND='${ssh_command[*]}' git push $publish_remote $branch"
  echo "  GIT_SSH_COMMAND='${ssh_command[*]}' git push $publish_remote v$version"
fi
