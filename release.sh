#!/bin/bash

set -e  # Exit on error

# Define paths
GRADLE_PROPERTIES="gradle.properties"
MODULE_NODE="androidNode"
MODULE_CLIENT="androidClient"
MODULE_NAME="$1"
APP_TYPE=""

echo "Bisq Android Mobile Release Script (node/client)"

if [ -z "$MODULE_NAME" ]; then
    echo "Please pass in the module name to release: node or client. e.g. './release.sh node'"
    exit 1
fi
if [ "$MODULE_NAME" == "client" ]; then
    APP_TYPE="client"
    MODULE_NAME="$MODULE_CLIENT"
elif [ "$MODULE_NAME" == "node" ]; then
    APP_TYPE="node"
    MODULE_NAME="$MODULE_NODE"
else
    echo "Invalid parameter, modules are node or client"
    exit 2
fi

# Extract current version
CURRENT_VERSION=$(grep "^$APP_TYPE.android.version=" "$GRADLE_PROPERTIES" | cut -d'=' -f2)
# Extract current version code
CURRENT_VERSION_CODE=$(grep "^$APP_TYPE.android.version.code=" "$GRADLE_PROPERTIES" | cut -d'=' -f2)
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))

# Split version into major, minor, patch
MAJOR=$(echo "$CURRENT_VERSION" | cut -d'.' -f1)
MINOR=$(echo "$CURRENT_VERSION" | cut -d'.' -f2)
PATCH=$(echo "$CURRENT_VERSION" | cut -d'.' -f3)

# Increment the patch version
NEW_PATCH=$((PATCH + 1))
NEW_VERSION="$MAJOR.$MINOR.$NEW_PATCH"

echo "$APP_TYPE: first we create a branch for the release - you will loose any uncommited changes"
git reset --hard
git checkout main
BRANCH="release/a${APP_TYPE}_${CURRENT_VERSION}"
# Create branch if it doesn't exist; otherwise just check it out
git checkout -b "$BRANCH" 2>/dev/null || git checkout "$BRANCH"
# Set upstream if needed; continue if it's already set
git push --set-upstream origin "$BRANCH" 2>/dev/null || echo "[info] Upstream already set for $BRANCH"

echo "$APP_TYPE: Building Android release out of your current branch"
git status

# Build AAB
./gradlew "$MODULE_NAME:clean"
./gradlew "$MODULE_NAME:bundleRelease"

# Tag and push
git tag "$APP_TYPE-release-$CURRENT_VERSION"
git push --tags

echo "Build complete. AAB is located at: $MODULE_NAME/build/outputs/bundle/release/"

# Update gradle.properties
sed -i '' "s/^$APP_TYPE.android.version=.*/$APP_TYPE.android.version=$NEW_VERSION/" "$GRADLE_PROPERTIES"
sed -i '' "s/^$APP_TYPE.android.version.code=.*/$APP_TYPE.android.version.code=$NEW_VERSION_CODE/" "$GRADLE_PROPERTIES"

echo "Updated $APP_TYPE.android.version to $NEW_VERSION to start its development"

# Commit the version bump
git add "$GRADLE_PROPERTIES"
git commit -m "Bump $APP_TYPE.android.version to $NEW_VERSION to start its development"


# --- Release notes metadata (APK) ---
print_release_apk_metadata() {
  # Prefer module-specific release APKs, then fallback to newest -release.apk in repo
  local apk
  apk="${APK_PATH:-$(ls -1t "$MODULE_NAME"/build/outputs/apk/release/*-release.apk 2>/dev/null | head -n1)}"
  if [ -z "$apk" ]; then
    apk="${APK_PATH:-$(find . -type f -name '*-release.apk' ! -name '*unaligned*' ! -name '*unsigned*' -print0 2>/dev/null | xargs -0 ls -t 2>/dev/null | head -n1)}"
  fi

  if [ -z "$apk" ]; then
    echo "[info] No release APK found to extract metadata. If you also build an APK, set APK_PATH or run assembleRelease."
    return 0
  fi

  local file bytes mib sha cert_raw cert
  file="$(basename "$apk")"

  # Version info: use values from gradle.properties we parsed above
  local vn vc
  vn="$CURRENT_VERSION"
  vc="$CURRENT_VERSION_CODE"

  # Size (bytes + MiB with high precision)
  if bytes=$(stat -f%z "$apk" 2>/dev/null); then :; elif bytes=$(stat -c%s "$apk" 2>/dev/null); then :; else bytes=$(wc -c <"$apk"); fi
  mib="$(echo "scale=23; $bytes/1048576" | bc -l)"

  # SHA-256 of the APK
  if sha=$(shasum -a 256 "$apk" 2>/dev/null | awk '{print $1}'); then :; else sha=$(sha256sum "$apk" 2>/dev/null | awk '{print $1}'); fi

  # Signing cert fingerprint (lowercase, no colons)
  if cert_raw=$(apksigner verify --print-certs --verbose "$apk" 2>/dev/null | awk -F': ' '/SHA-256 digest/{print $2}'); then :;
  else cert_raw=$(keytool -list -printcert -jarfile "$apk" 2>/dev/null | awk -F': ' '/SHA256:/{print $2}'); fi
  cert="$(echo "$cert_raw" | tr -d ':' | tr 'A-F' 'a-f')"

  echo ""
  echo "Release artifact metadata"
  echo " - File: $file"
  echo " - Version: ${vn:-unknown} (versionCode ${vc:-unknown})"
  echo " - Size: ${bytes} bytes (${mib} MiB)"
  echo " - SHA-256: ${sha:-unavailable}"
  echo " - Signing cert: ${cert:-unavailable}"
}

# Print APK metadata (if an APK exists)
print_release_apk_metadata || true

echo "All done, please create a pull request and merge this changes :)"

exit 0