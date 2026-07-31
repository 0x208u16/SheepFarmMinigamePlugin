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
github_token=""

GITHUB_HTTP_STATUS=""
GITHUB_RESPONSE_BODY=""
GITHUB_RESPONSE_HEADERS=""

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
  github_api_get "https://api.github.com/repos/${owner}/${repo}/releases/tags/${tag}" || true
  [[ "$GITHUB_HTTP_STATUS" == "200" ]]
}

release_page_exists() {
  local owner="$1"
  local repo="$2"
  local tag="$3"
  local page_status

  page_status="$(curl -sS -L -o /dev/null -w "%{http_code}" "https://github.com/${owner}/${repo}/releases/tag/${tag}" 2>/dev/null || true)"
  [[ "$page_status" == "200" ]]
}

github_api_patch_json() {
  local url="$1"
  local json_body="$2"
  local body_file
  local header_file
  local response_file
  local http_status
  local curl_args

  body_file="$(mktemp)"
  header_file="$(mktemp)"
  response_file="$(mktemp)"
  printf '%s' "$json_body" > "$body_file"

  curl_args=(
    -sS
    -L
    -X PATCH
    -H "Accept: application/vnd.github+json"
    -H "X-GitHub-Api-Version: 2022-11-28"
    -H "User-Agent: SheepMergePublishScript/1.0"
    -H "Content-Type: application/json"
  )

  if [[ -n "$github_token" ]]; then
    curl_args+=( -H "Authorization: Bearer ${github_token}" )
  fi

  http_status="$(curl "${curl_args[@]}" -D "$header_file" -o "$response_file" -w "%{http_code}" --data-binary @"$body_file" "$url" 2>/dev/null || true)"

  GITHUB_HTTP_STATUS="$http_status"
  GITHUB_RESPONSE_BODY="$(cat "$response_file")"
  GITHUB_RESPONSE_HEADERS="$(cat "$header_file")"

  rm -f "$body_file" "$header_file" "$response_file"

  [[ "$http_status" =~ ^[0-9]{3}$ ]]
}

github_api_get() {
  local url="$1"
  local body_file
  local header_file
  local http_status
  local curl_args

  body_file="$(mktemp)"
  header_file="$(mktemp)"

  curl_args=(
    -sS
    -L
    -H "Accept: application/vnd.github+json"
    -H "X-GitHub-Api-Version: 2022-11-28"
    -H "User-Agent: SheepMergePublishScript/1.0"
  )

  if [[ -n "$github_token" ]]; then
    curl_args+=( -H "Authorization: Bearer ${github_token}" )
  fi

  http_status="$(curl "${curl_args[@]}" -D "$header_file" -o "$body_file" -w "%{http_code}" "$url" 2>/dev/null || true)"

  GITHUB_HTTP_STATUS="$http_status"
  GITHUB_RESPONSE_BODY="$(cat "$body_file")"
  GITHUB_RESPONSE_HEADERS="$(cat "$header_file")"

  rm -f "$body_file" "$header_file"

  [[ "$http_status" =~ ^[0-9]{3}$ ]]
}

get_header_value() {
  local headers="$1"
  local key="$2"

  awk -v key="$key" '
    BEGIN { IGNORECASE = 1 }
    {
      line = $0
      gsub("\r", "", line)
      split(line, parts, ":")
      if (tolower(parts[1]) == tolower(key)) {
        sub("^[^:]*:[[:space:]]*", "", line)
        print line
        exit
      }
    }
  ' <<< "$headers"
}

generate_commit_changelog() {
  local tag="$1"
  local previous_tag=""
  local range=""
  local changelog=""

  previous_tag="$(git describe --tags --abbrev=0 --match 'v*' "${tag}^" 2>/dev/null || true)"
  if [[ -n "$previous_tag" ]]; then
    range="${previous_tag}..${tag}"
  else
    range="HEAD"
  fi

  changelog="$(git log --reverse --pretty=format:'- %s (%h)' "$range" 2>/dev/null || true)"
  if [[ -z "$changelog" ]]; then
    changelog='- No commit history available for this release.'
  fi

  printf '## Changes\n\n'
  printf '%s\n' "$changelog"
}

update_release_changelog() {
  local owner="$1"
  local repo="$2"
  local tag="$3"
  local release_id=""
  local changelog_body=""
  local json_payload=""

  if ! release_exists "$owner" "$repo" "$tag"; then
    return 0
  fi

  changelog_body="$(generate_commit_changelog "$tag")"
  release_id="$(python - <<'PY' "$GITHUB_RESPONSE_BODY"
import json
import sys
payload = json.loads(sys.argv[1])
print(payload.get('id', ''))
PY
)"

  if [[ -z "$release_id" ]]; then
    echo "Could not determine release ID for $tag; skipping changelog update." >&2
    return 1
  fi

  json_payload="$(python - <<'PY' "$changelog_body"
import json
import sys
print(json.dumps({'body': sys.argv[1]}))
PY
)"

  if ! github_api_patch_json "https://api.github.com/repos/${owner}/${repo}/releases/${release_id}" "$json_payload"; then
    echo "GitHub API returned an unexpected response while updating release changelog for $tag." >&2
    return 1
  fi

  if [[ "$GITHUB_HTTP_STATUS" != "200" ]]; then
    echo "Failed to update release changelog for $tag (HTTP $GITHUB_HTTP_STATUS)." >&2
    return 1
  fi

  echo "Updated release changelog for $tag from git commits."
}

load_github_token() {
  if [[ -n "${GITHUB_TOKEN:-}" ]]; then
    github_token="$GITHUB_TOKEN"
    return
  fi

  if [[ -n "${GH_TOKEN:-}" ]]; then
    github_token="$GH_TOKEN"
    return
  fi

  if command -v gh >/dev/null 2>&1; then
    github_token="$(gh auth token 2>/dev/null || true)"
  fi
}

verify_remote_access() {
  if ! GIT_SSH_COMMAND="${ssh_command[*]}" git ls-remote --heads "$publish_remote" >/dev/null 2>&1; then
    echo "Unable to access $publish_remote using $KEY_PATH" >&2
    echo "Check that the private key is authorized for the repository and that the repo exists." >&2
    exit 1
  fi
}

ensure_branch_can_push() {
  local branch="$1"
  local remote_sha

  if ! GIT_SSH_COMMAND="${ssh_command[*]}" git fetch --quiet "$publish_remote" "$branch"; then
    echo "Failed to fetch remote branch state for $branch from $publish_remote." >&2
    echo "Resolve connectivity/auth issues and try again." >&2
    exit 1
  fi

  remote_sha="$(git rev-parse -q --verify FETCH_HEAD || true)"
  if [[ -z "$remote_sha" ]]; then
    echo "Could not resolve remote branch state for $branch." >&2
    exit 1
  fi

  if ! git merge-base --is-ancestor "$remote_sha" HEAD; then
    echo "Local branch $branch is behind or diverged from remote." >&2
    echo "Run: git pull --rebase origin $branch" >&2
    echo "Then rerun: scripts/publish-release.sh $version" >&2
    exit 1
  fi
}

get_release_workflow_state() {
  local owner="$1"
  local repo="$2"
  local sha="$3"
  local response

  github_api_get "https://api.github.com/repos/${owner}/${repo}/actions/runs?head_sha=${sha}&per_page=20" || true
  if [[ "$GITHUB_HTTP_STATUS" != "200" ]]; then
    return 1
  fi
  response="$GITHUB_RESPONSE_BODY"

  RELEASE_WORKFLOW_RESPONSE="$response" python - <<'PY'
import json
import os

payload = json.loads(os.environ['RELEASE_WORKFLOW_RESPONSE'])
for run in payload.get('workflow_runs', []):
    if run.get('name') == 'Publish Release' or run.get('path') == '.github/workflows/release.yml':
        status = run.get('status') or 'unknown'
        conclusion = run.get('conclusion') or ''
        print(f"{status}:{conclusion}")
        break
else:
    print('missing:')
PY
}

wait_for_release() {
  local owner="$1"
  local repo="$2"
  local tag="$3"
  local sha="$4"

  for (( attempt=1; attempt<=RELEASE_POLL_RETRIES; attempt++ )); do
    if release_exists "$owner" "$repo" "$tag"; then
      echo "GitHub Release $tag is live."
      return 0
    fi

    if [[ "$GITHUB_HTTP_STATUS" == "403" ]]; then
      local remaining reset
      remaining="$(get_header_value "$GITHUB_RESPONSE_HEADERS" "x-ratelimit-remaining")"
      reset="$(get_header_value "$GITHUB_RESPONSE_HEADERS" "x-ratelimit-reset")"
      if [[ "$remaining" == "0" ]]; then
        local reset_at
        reset_at="unknown"
        if [[ -n "$reset" && "$reset" =~ ^[0-9]+$ ]]; then
          reset_at="$(date -u -d "@$reset" '+%Y-%m-%d %H:%M:%S UTC' 2>/dev/null || echo "$reset")"
        fi
        echo "GitHub API rate limit reached while checking release $tag (remaining=$remaining, reset=$reset_at)." >&2

        if release_page_exists "$owner" "$repo" "$tag"; then
          echo "GitHub Release $tag is visible on the web release page."
          return 0
        fi

        if [[ -z "$github_token" ]]; then
          echo "Cannot continue reliable polling without API quota. Stopping early so release workflow can finish in the background." >&2
          return 3
        fi
      else
        echo "GitHub API returned 403 while checking release $tag. This usually means missing/insufficient API auth for a private repository." >&2
        return 2
      fi
      if [[ -z "$github_token" ]]; then
        echo "Set GITHUB_TOKEN (or GH_TOKEN) and rerun publish to avoid unauthenticated API limits." >&2
      fi
    fi

    local workflow_state
    workflow_state="$(get_release_workflow_state "$owner" "$repo" "$sha" || true)"
    case "$workflow_state" in
      completed:success)
        echo "Release workflow completed successfully but release $tag is not visible yet; continuing to poll."
        ;;
      completed:failure|completed:cancelled|completed:timed_out|completed:action_required)
        echo "Release workflow failed for $tag ($workflow_state)." >&2
        return 2
        ;;
      in_progress:*|queued:*|requested:*|waiting:*|pending:*)
        echo "Release workflow status for $tag: $workflow_state"
        ;;
      missing:*)
        echo "Release workflow for $tag has not appeared yet."
        ;;
    esac
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

load_github_token
if [[ -z "$github_token" ]]; then
  echo "No GitHub API token detected; release polling will be limited by anonymous GitHub API rate limits." >&2
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

branch="$(git branch --show-current)"
if [[ -z "$branch" ]]; then
  echo "Could not determine current branch." >&2
  exit 1
fi

if [[ $do_push -eq 1 ]]; then
  ensure_branch_can_push "$branch"
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

  tag_sha="$(git rev-parse "v$version")"
  if ! wait_for_release "$github_owner" "$github_repo" "v$version" "$tag_sha"; then
    wait_status=$?
    if [[ $wait_status -eq 3 ]]; then
      echo "Release verification paused because GitHub API limit was exhausted. Tag and commit were pushed successfully." >&2
      echo "Set GITHUB_TOKEN (or GH_TOKEN) and rerun the same publish command to resume verification." >&2
      exit 0
    fi

    cleanup_on_failure "$version"
    if [[ $wait_status -eq 2 ]]; then
      echo "GitHub Actions release workflow failed for v$version. Cleaned up failed publish tag and local state." >&2
    else
      echo "GitHub Release v$version did not publish successfully in time. Cleaned up failed publish tag and local state." >&2
    fi
    echo "Check the GitHub Actions workflow for release failures." >&2
    exit 1
  fi

  if ! update_release_changelog "$github_owner" "$github_repo" "v$version"; then
    echo "Published release v$version but could not update its changelog from git commits." >&2
  fi

  echo "Published release v$version and pushed branch commit successfully."
else
  echo "Created commit and tag locally. Push when ready:"
  echo "  GIT_SSH_COMMAND='${ssh_command[*]}' git push $publish_remote $branch"
  echo "  GIT_SSH_COMMAND='${ssh_command[*]}' git push $publish_remote v$version"
fi
