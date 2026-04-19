#!/usr/bin/env bash
set -euo pipefail

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) is required." >&2
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "GitHub CLI is not authenticated. Run: gh auth login" >&2
  exit 1
fi

CREDENTIALS_FILE="${1:-}"
TARGET_REPO="${2:-}"

if [[ -z "$CREDENTIALS_FILE" ]]; then
  for path in \
    "./signing-credentials/signing-credentials.json" \
    "./signing-credentials.json" \
    "$HOME/Downloads/signing-credentials/signing-credentials.json" \
    "$HOME/Downloads/signing-credentials.json"
  do
    if [[ -f "$path" ]]; then
      CREDENTIALS_FILE="$path"
      break
    fi
  done
fi

if [[ -z "$CREDENTIALS_FILE" || ! -f "$CREDENTIALS_FILE" ]]; then
  echo "Usage: $0 <path-to-signing-credentials.json> [owner/repo]" >&2
  exit 1
fi

if command -v jq >/dev/null 2>&1; then
  KEYSTORE_BASE64=$(jq -r '.keystore_base64' "$CREDENTIALS_FILE")
  KEYSTORE_PASSWORD=$(jq -r '.keystore_password' "$CREDENTIALS_FILE")
  KEY_ALIAS=$(jq -r '.key_alias' "$CREDENTIALS_FILE")
  KEY_PASSWORD=$(jq -r '.key_password' "$CREDENTIALS_FILE")
else
  KEYSTORE_BASE64=$(grep -o '"keystore_base64"[[:space:]]*:[[:space:]]*"[^"]*"' "$CREDENTIALS_FILE" | sed 's/.*: *"\([^"]*\)"/\1/')
  KEYSTORE_PASSWORD=$(grep -o '"keystore_password"[[:space:]]*:[[:space:]]*"[^"]*"' "$CREDENTIALS_FILE" | sed 's/.*: *"\([^"]*\)"/\1/')
  KEY_ALIAS=$(grep -o '"key_alias"[[:space:]]*:[[:space:]]*"[^"]*"' "$CREDENTIALS_FILE" | sed 's/.*: *"\([^"]*\)"/\1/')
  KEY_PASSWORD=$(grep -o '"key_password"[[:space:]]*:[[:space:]]*"[^"]*"' "$CREDENTIALS_FILE" | sed 's/.*: *"\([^"]*\)"/\1/')
fi

if [[ -z "$KEYSTORE_BASE64" || -z "$KEYSTORE_PASSWORD" || -z "$KEY_ALIAS" || -z "$KEY_PASSWORD" ]]; then
  echo "Could not parse all required signing values from $CREDENTIALS_FILE" >&2
  exit 1
fi

if [[ -z "$TARGET_REPO" ]]; then
  TARGET_REPO=$(gh repo view --json nameWithOwner -q '.nameWithOwner')
fi

printf '%s' "$KEYSTORE_BASE64" | gh secret set ANDROID_KEYSTORE_BASE64 --repo "$TARGET_REPO"
printf '%s' "$KEYSTORE_PASSWORD" | gh secret set ANDROID_KEYSTORE_PASSWORD --repo "$TARGET_REPO"
printf '%s' "$KEY_ALIAS" | gh secret set ANDROID_KEY_ALIAS --repo "$TARGET_REPO"
printf '%s' "$KEY_PASSWORD" | gh secret set ANDROID_KEY_PASSWORD --repo "$TARGET_REPO"

echo "Saved Android signing secrets to $TARGET_REPO"
