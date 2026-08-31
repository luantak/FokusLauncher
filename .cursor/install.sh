#!/usr/bin/env bash
# Idempotent Cloud Agent setup for the Fokus Launcher Android project.
# Installs the Android SDK components CI relies on (JDK 21 is already provided by
# the base image) and points Gradle at them via local.properties. Safe to re-run.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="11076708"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"

# SDK packages: android-36 (targetSdk, matches CI) + android-37.0 (compileSdk) + build-tools.
SDK_PACKAGES=(
  "platform-tools"
  "platforms;android-36"
  "platforms;android-37.0"
  "build-tools;36.0.0"
)

echo "==> Verifying JDK"
if ! command -v javac >/dev/null 2>&1; then
  echo "ERROR: JDK not found on PATH. A JDK 21 toolchain is required." >&2
  exit 1
fi
java -version

echo "==> Ensuring Android command-line tools at ${ANDROID_SDK_ROOT}"
SDKMANAGER="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "${SDKMANAGER}" ]; then
  mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools"
  tmp_zip="$(mktemp --suffix=.zip)"
  echo "    downloading command-line tools"
  curl -fsSL -o "${tmp_zip}" "${CMDLINE_TOOLS_URL}"
  rm -rf "${ANDROID_SDK_ROOT}/cmdline-tools/latest" "${ANDROID_SDK_ROOT}/cmdline-tools/tmp"
  unzip -q "${tmp_zip}" -d "${ANDROID_SDK_ROOT}/cmdline-tools/tmp"
  mv "${ANDROID_SDK_ROOT}/cmdline-tools/tmp/cmdline-tools" "${ANDROID_SDK_ROOT}/cmdline-tools/latest"
  rmdir "${ANDROID_SDK_ROOT}/cmdline-tools/tmp" 2>/dev/null || true
  rm -f "${tmp_zip}"
fi

echo "==> Accepting SDK licenses"
yes | "${SDKMANAGER}" --sdk_root="${ANDROID_SDK_ROOT}" --licenses >/dev/null 2>&1 || true

echo "==> Installing SDK packages"
"${SDKMANAGER}" --sdk_root="${ANDROID_SDK_ROOT}" "${SDK_PACKAGES[@]}"

echo "==> Writing local.properties"
printf 'sdk.dir=%s\n' "${ANDROID_SDK_ROOT}" > "${REPO_ROOT}/local.properties"

echo "==> Warming Gradle build (dependency + KSP caches)"
cd "${REPO_ROOT}"
./gradlew --no-daemon :app:assembleDebug

echo "==> Setup complete. SDK at ${ANDROID_SDK_ROOT}"
