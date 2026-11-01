#!/usr/bin/env bash
# One-time setup for the Release workflow. Run on the Mac that has ~/samar-keys, from the samar folder:
#
#   bash scripts/setup-release-secrets.sh
#
# Needs the GitHub CLI (brew install gh, then gh auth login).
# Creates a "release" environment that only the main branch can use, and stores the upload key
# and its passwords there as secrets. Nothing is printed and nothing is written to this folder.
set -euo pipefail

repo=hystericmoon/samar
props="$HOME/samar-keys/upload.properties"
here="$(cd "$(dirname "$0")/.." && pwd)"

command -v gh >/dev/null || { echo "GitHub CLI not found. Install it: brew install gh, then gh auth login"; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "Not signed in to GitHub. Run: gh auth login"; exit 1; }
[[ -f "$props" ]] || { echo "Can't find $props"; exit 1; }

get() { grep -E "^[[:space:]]*$1[[:space:]]*=" "$props" | head -1 | cut -d= -f2- | sed 's/^[[:space:]]*//; s/[[:space:]]*$//'; }

store="$(get storeFile)"
# Gradle reads a relative storeFile from the app module folder.
[[ "$store" == /* ]] || store="$here/app/$store"
[[ -f "$store" ]] || { echo "Keystore not found: $store"; exit 1; }
for k in storePassword keyAlias keyPassword; do
  [[ -n "$(get "$k")" ]] || { echo "$k is missing from $props"; exit 1; }
done

echo "Creating the protected 'release' environment (main branch only)…"
gh api -X PUT "repos/$repo/environments/release" \
  -F 'deployment_branch_policy[protected_branches]=false' \
  -F 'deployment_branch_policy[custom_branch_policies]=true' >/dev/null
gh api -X POST "repos/$repo/environments/release/deployment-branch-policies" \
  -f name=main -f type=branch >/dev/null 2>&1 || true   # already there on a re-run

echo "Storing the key and passwords as environment secrets…"
base64 < "$store" | tr -d '\n' | gh secret set SAMAR_KEYSTORE_BASE64 --env release --repo "$repo"
get storePassword | tr -d '\n' | gh secret set SAMAR_STORE_PASSWORD --env release --repo "$repo"
get keyAlias      | tr -d '\n' | gh secret set SAMAR_KEY_ALIAS      --env release --repo "$repo"
get keyPassword   | tr -d '\n' | gh secret set SAMAR_KEY_PASSWORD   --env release --repo "$repo"

echo "Done. Releases can now be run from GitHub: Actions → Release → Run workflow."
