#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -d "$SCRIPT_DIR/tools/src" ]; then
  DEFAULT_INSTALL_ROOT="$HOME/Desktop/C2"
  SUPPORT_DIR="$SCRIPT_DIR"
else
  DEFAULT_INSTALL_ROOT="$SCRIPT_DIR"
  SUPPORT_DIR="$SCRIPT_DIR/.c2-apple-silicon-installer"
fi

INSTALL_ROOT="${C2_INSTALL_ROOT:-$DEFAULT_INSTALL_ROOT}"
GAME_DIR="$INSTALL_ROOT/c2-patch"
TOOLS_DIR="${C2_TOOLS_DIR:-$SUPPORT_DIR/tools}"
CACHE_DIR="$SUPPORT_DIR/.cache"
WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/c2-arm-update.XXXXXX")"

C2_PATCH_URL="${C2_PATCH_URL:-https://github.com/shayklos/c2-patch/archive/refs/heads/stable.zip}"
C2_PATCH_BRANCH_API_URL="${C2_PATCH_BRANCH_API_URL:-https://api.github.com/repos/shayklos/c2-patch/branches/stable}"
C2_PATCH_BACKUP_URL="${C2_PATCH_BACKUP_URL:-https://data.catgc.com/c2-patch-stable%20%2821.06.26%29.zip}"
INSTALLER_VERSION="v1.4.2"
INSTALLER_RELEASE_API_URL="${INSTALLER_RELEASE_API_URL:-https://api.github.com/repos/Furrypaw/c2-apple-silicon-installer/releases/latest}"
ZULU_JDK_URL="${ZULU_JDK_URL:-https://cdn.azul.com/zulu/bin/zulu8.94.0.17-ca-jdk8.0.492-macosx_aarch64.zip}"
ASM_URL="${ASM_URL:-https://repo1.maven.org/maven2/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar}"
ASM_COMMONS_URL="${ASM_COMMONS_URL:-https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/9.7.1/asm-commons-9.7.1.jar}"
LWJGL_JAR_URL="${LWJGL_JAR_URL:-https://files.betacraft.uk/launcher/v2/assets/libraries/lwjgl/lwjgl-2.9.3-macos-aarch64.jar}"
LWJGL_PLATFORM_URL="${LWJGL_PLATFORM_URL:-https://files.betacraft.uk/launcher/v2/assets/libraries/lwjgl/lwjgl-platform-2.9.3-macos-aarch64.jar}"
BASS_OSX_URL="${BASS_OSX_URL:-https://www.un4seen.com/files/bass24-osx.zip}"

cleanup() {
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

say() {
  printf '\n%s\n' "$1"
}

pause_if_tty() {
  if [ -t 0 ]; then
    read -r -p "Press Enter to close..."
  fi
}

download() {
  local url="$1"
  local out="$2"
  local tmp="$out.partial"
  local size
  if [ -s "$out" ]; then
    printf 'Using cached %s\n' "$(basename "$out")"
    return
  fi
  printf 'Downloading %s ... ' "$(basename "$out")"
  rm -f "$tmp"
  if curl --fail --location --silent --show-error "$url" -o "$tmp"; then
    mv "$tmp" "$out"
    size="$(du -h "$out" | awk '{print $1}')"
    printf 'done (%s)\n' "$size"
  else
    rm -f "$tmp"
    printf 'failed\n'
    return 1
  fi
}

latest_upstream_sha() {
  curl --fail --location --silent --show-error "$C2_PATCH_BRANCH_API_URL" \
    | sed -n 's/.*"sha": *"\([0-9a-f]\{40\}\)".*/\1/p' \
    | head -n 1
}

version_code() {
  local v="${1#v}"
  local major minor patch
  v="${v%%-*}"
  IFS=. read -r major minor patch <<EOF
$v
EOF
  major="${major:-0}"
  minor="${minor:-0}"
  patch="${patch:-0}"
  case "$major" in ''|*[!0-9]*) major=0 ;; esac
  case "$minor" in ''|*[!0-9]*) minor=0 ;; esac
  case "$patch" in ''|*[!0-9]*) patch=0 ;; esac
  printf '%d%03d%03d\n' "$major" "$minor" "$patch"
}

version_gt() {
  [ "$(version_code "$1")" -gt "$(version_code "$2")" ]
}

read_installer_version() {
  if [ -f "$SUPPORT_DIR/version" ]; then
    tr -d '[:space:]' < "$SUPPORT_DIR/version"
  else
    printf '%s\n' "$INSTALLER_VERSION"
  fi
}

latest_installer_release() {
  local metadata="$WORK_DIR/installer-release.json"
  curl --fail --location --silent --show-error "$INSTALLER_RELEASE_API_URL" -o "$metadata"
  INSTALLER_REMOTE_TAG="$(sed -n 's/.*"tag_name": *"\([^"]*\)".*/\1/p' "$metadata" | head -n 1)"
  INSTALLER_REMOTE_ZIP_URL="$(sed -n 's/.*"browser_download_url": *"\([^"]*c2-apple-silicon-installer[^"]*\.zip\)".*/\1/p' "$metadata" | head -n 1)"
  [ -n "$INSTALLER_REMOTE_TAG" ] && [ -n "$INSTALLER_REMOTE_ZIP_URL" ]
}

zip_comment_sha() {
  unzip -z "$1" 2>/dev/null \
    | sed -n 's/^\([0-9a-f]\{40\}\)$/\1/p' \
    | head -n 1
}

prepare_installer_self_update() {
  ACTIVE_TOOLS_DIR="$TOOLS_DIR"
  ACTIVE_UPDATER_SCRIPT="$0"
  ACTIVE_INSTALLER_VERSION="$(read_installer_version)"
  INSTALLER_UPDATE_AVAILABLE=0

  if [ "${C2_SKIP_INSTALLER_UPDATE:-0}" = "1" ]; then
    echo "Installer self-update check skipped."
    return
  fi

  say "Checking Furrypaw/c2-apple-silicon-installer latest release."
  if ! latest_installer_release; then
    echo "Could not check the installer release. Continuing with local installer tools."
    return
  fi

  echo "Local installer version: ${ACTIVE_INSTALLER_VERSION:-unknown}"
  echo "Latest installer version: $INSTALLER_REMOTE_TAG"

  if [ "${C2_FORCE_INSTALLER_UPDATE:-0}" != "1" ] && ! version_gt "$INSTALLER_REMOTE_TAG" "$ACTIVE_INSTALLER_VERSION"; then
    return
  fi

  local release_zip="$CACHE_DIR/c2-apple-silicon-installer-$INSTALLER_REMOTE_TAG.zip"
  local release_dir="$WORK_DIR/installer-release"
  local release_root
  download "$INSTALLER_REMOTE_ZIP_URL" "$release_zip"
  mkdir -p "$release_dir"
  unzip -q "$release_zip" -d "$release_dir"
  release_root="$(find "$release_dir" -maxdepth 2 -type d -name c2-apple-silicon-installer | head -n 1)"
  if [ -z "$release_root" ] || [ ! -d "$release_root/tools/src" ] \
      || [ ! -f "$release_root/Update Cultris II for Apple Silicon.command" ]; then
    echo "Downloaded installer release did not contain the expected tools. Continuing with local tools."
    return
  fi

  ACTIVE_TOOLS_DIR="$release_root/tools"
  ACTIVE_UPDATER_SCRIPT="$release_root/Update Cultris II for Apple Silicon.command"
  ACTIVE_INSTALLER_VERSION="$INSTALLER_REMOTE_TAG"
  INSTALLER_UPDATE_AVAILABLE=1
  echo "Using installer tools from $INSTALLER_REMOTE_TAG for this update."
}

copy_first() {
  local name="$1"
  local out="$2"
  local found
  found="$(find "$WORK_DIR/lwjgl-platform" -name "$name" -type f | head -n 1)"
  if [ -n "$found" ]; then
    cp "$found" "$out"
  fi
}

install_bass_music_support() {
  local game_dir="$1"
  local lib_arm="$2"
  local java_home="$3"
  local bridge_src="$TOOLS_DIR/native/c2_bass_music.c"
  local bass_dir="$WORK_DIR/bass-osx"
  local bass_lib

  mkdir -p "$bass_dir"
  unzip -q "$CACHE_DIR/bass24-osx.zip" -d "$bass_dir"
  bass_lib="$(find "$bass_dir" -name libbass.dylib -type f | head -n 1)"
  if [ -z "$bass_lib" ]; then
    echo "Could not find libbass.dylib in the BASS macOS package."
    return 1
  fi

  if command -v lipo >/dev/null 2>&1; then
    lipo "$bass_lib" -thin arm64 -output "$lib_arm/libbass.dylib" 2>/dev/null || cp "$bass_lib" "$lib_arm/libbass.dylib"
  else
    cp "$bass_lib" "$lib_arm/libbass.dylib"
  fi

  if ! command -v clang >/dev/null 2>&1; then
    echo "Apple clang was not found, so MO3 music support could not be built."
    return 1
  fi
  clang -dynamiclib -arch arm64 -O2 -mmacosx-version-min=11.0 \
    -I"$java_home/include" \
    -I"$java_home/include/darwin" \
    "$bridge_src" \
    -o "$lib_arm/libC2BassMusic.jnilib"

  codesign -s - "$lib_arm/libbass.dylib" "$lib_arm/libC2BassMusic.jnilib" >/dev/null 2>&1 || true

  mkdir -p "$game_dir/settings"
  if [ ! -f "$game_dir/settings/music-enabled.txt" ]; then
    printf '1\n' > "$game_dir/settings/music-enabled.txt"
  fi
}

extract_class() {
  local class_path="$1"
  local out="$WORK_DIR/original-classes/$class_path.class"
  mkdir -p "$(dirname "$out")"
  unzip -p "$NEW_GAME_DIR/cultris2.jar" "$class_path.class" > "$out"
}

patch_class() {
  local patcher="$1"
  local class_path="$2"
  local in="$WORK_DIR/original-classes/$class_path.class"
  local out="$PATCHED_CLASSES/$class_path.class"
  mkdir -p "$(dirname "$out")"
  "$JAVA_BIN" -cp "$PATCHER_CP" "$patcher" "$in" "$out"
}

find_java_tools() {
  JAVA_BIN="$(find "$NEW_GAME_DIR/resources/runtime" -path '*/Contents/Home/bin/java' -type f | head -n 1)"
  JAVAC_BIN="$(find "$NEW_GAME_DIR/resources/runtime" -path '*/Contents/Home/bin/javac' -type f | head -n 1)"
  JAR_BIN="$(find "$NEW_GAME_DIR/resources/runtime" -path '*/Contents/Home/bin/jar' -type f | head -n 1)"
  if [ -n "$JAVA_BIN" ]; then
    JAVA_HOME_DIR="$(cd "$(dirname "$JAVA_BIN")/.." && pwd)"
  fi
}

preserve_user_data() {
  local old_dir="$1"
  local new_dir="$2"

  if [ -d "$old_dir/settings" ]; then
    mkdir -p "$new_dir/settings"
    cp -R "$old_dir/settings/." "$new_dir/settings/"
  fi

  if [ -d "$old_dir/replays" ]; then
    mkdir -p "$new_dir/replays"
    find "$old_dir/replays" -maxdepth 1 -type f \
      ! -name '*.jar' \
      ! -name 'how_replays_work.txt' \
      -exec cp -p {} "$new_dir/replays/" \;
  fi
}

ensure_combo_goal_helper_setting() {
  local settings_dir="$1"
  local file="$settings_dir/comboGoalHelper.txt"
  local enabled goal start print_max theoretical

  if [ ! -f "$file" ]; then
    return
  fi

  enabled="$(sed -n '2p' "$file" | tr -d '\r' | tr -d '[:space:]')"
  goal="$(sed -n '5p' "$file" | tr -d '\r' | tr -d '[:space:]')"
  start="$(sed -n '8p' "$file" | tr -d '\r' | tr -d '[:space:]')"
  print_max="$(sed -n '11p' "$file" | tr -d '\r' | tr -d '[:space:]')"
  theoretical="$(sed -n '14p' "$file" | tr -d '\r' | tr -d '[:space:]')"

  case "$enabled" in ''|*[!0-9-]*) enabled="0" ;; esac
  case "$goal" in ''|*[!0-9-]*) goal="14" ;; esac
  case "$start" in ''|*[!0-9-]*) start="9" ;; esac
  case "$print_max" in ''|*[!0-9-]*) print_max="1" ;; esac
  case "$theoretical" in ''|*[!0-9-]*) theoretical="15" ;; esac

  {
    printf 'Enabled\n%s\n\n' "$enabled"
    printf 'Combo goal\n%s\n\n' "$goal"
    printf 'Combo at which to start printing the stats in chat\n%s\n\n' "$start"
    printf 'Whether to print max hypothetical combo\n%s\n\n' "$print_max"
    printf "Theoretical max combo. You shouldn't change this unless you're going for the 16\n%s\n" "$theoretical"
  } > "$file"
}

if [ "$(uname -m)" != "arm64" ]; then
  echo "This updater is for Apple Silicon Macs."
  echo "This Mac reports: $(uname -m)"
  pause_if_tty
  exit 1
fi

if [ ! -d "$GAME_DIR" ] || [ ! -f "$GAME_DIR/cultris2.jar" ]; then
  echo "Could not find an installed Cultris II folder at:"
  echo "$GAME_DIR"
  echo
  echo "Run the installer first, or set C2_INSTALL_ROOT to your C2 folder."
  pause_if_tty
  exit 1
fi

if [ ! -d "$TOOLS_DIR/src" ]; then
  echo "Could not find Apple Silicon patch tools at:"
  echo "$TOOLS_DIR"
  echo
  echo "Download the full release ZIP again and run the updater from that folder."
  pause_if_tty
  exit 1
fi

if pgrep -f "$GAME_DIR/resources/runtime/.*/bin/java.*cultris2.jar" >/dev/null 2>&1; then
  echo "Cultris II appears to be running. Please quit the game before updating."
  pause_if_tty
  exit 1
fi

mkdir -p "$CACHE_DIR"
prepare_installer_self_update
TOOLS_DIR="$ACTIVE_TOOLS_DIR"
TOOLS_STAGING_DIR="$WORK_DIR/installer-support"
mkdir -p "$TOOLS_STAGING_DIR"
cp -R "$TOOLS_DIR" "$TOOLS_STAGING_DIR/tools"

LOCAL_SHA=""
if [ -f "$GAME_DIR/.c2-upstream-stable-sha" ]; then
  LOCAL_SHA="$(tr -d '[:space:]' < "$GAME_DIR/.c2-upstream-stable-sha")"
fi

say "Checking shayklos/c2-patch stable branch."
REMOTE_SHA="$(latest_upstream_sha || true)"
if [ -z "$REMOTE_SHA" ]; then
  if [ "${C2_FORCE_UPDATE:-0}" = "1" ]; then
    REMOTE_SHA="forced-$(date +%Y%m%d%H%M%S)"
    echo "Could not check the upstream stable branch. Forcing an update from the configured ZIP."
  else
    echo "Could not check the upstream stable branch."
    echo "You can force an update from the configured ZIP with:"
    echo "C2_FORCE_UPDATE=1 \"$0\""
    pause_if_tty
    exit 1
  fi
fi

echo "Local upstream SHA: ${LOCAL_SHA:-not recorded}"
echo "Latest upstream SHA: $REMOTE_SHA"

if [ "${C2_FORCE_UPDATE:-0}" != "1" ] && [ "$INSTALLER_UPDATE_AVAILABLE" != "1" ] \
    && [ -n "$LOCAL_SHA" ] && [ "$LOCAL_SHA" = "$REMOTE_SHA" ]; then
  echo
  echo "Already up to date."
  pause_if_tty
  exit 0
fi

say "Downloading update files."
PATCH_ZIP="$CACHE_DIR/c2-patch-stable-$REMOTE_SHA.zip"
if ! download "$C2_PATCH_URL" "$PATCH_ZIP"; then
  echo "Primary c2-patch download failed. Trying backup URL."
  download "$C2_PATCH_BACKUP_URL" "$PATCH_ZIP"
fi
download "$ASM_URL" "$CACHE_DIR/asm-9.7.1.jar"
download "$ASM_COMMONS_URL" "$CACHE_DIR/asm-commons-9.7.1.jar"
download "$LWJGL_JAR_URL" "$CACHE_DIR/lwjgl-2.9.3-macos-aarch64.jar"
download "$LWJGL_PLATFORM_URL" "$CACHE_DIR/lwjgl-platform-2.9.3-macos-aarch64.jar"
download "$BASS_OSX_URL" "$CACHE_DIR/bass24-osx.zip"

say "Preparing updated c2-patch."
unzip -q "$PATCH_ZIP" -d "$WORK_DIR"
NEW_GAME_DIR="$WORK_DIR/c2-patch-stable"
if [ ! -f "$NEW_GAME_DIR/cultris2.jar" ]; then
  echo "Downloaded c2-patch ZIP did not contain c2-patch-stable/cultris2.jar."
  pause_if_tty
  exit 1
fi

mkdir -p "$NEW_GAME_DIR/resources/runtime"
if [ -d "$GAME_DIR/resources/runtime" ] && find "$GAME_DIR/resources/runtime" -path '*/Contents/Home/bin/javac' -type f | grep -q .; then
  cp -R "$GAME_DIR/resources/runtime/." "$NEW_GAME_DIR/resources/runtime/"
else
  download "$ZULU_JDK_URL" "$CACHE_DIR/zulu8-arm64-jdk.zip"
  unzip -q "$CACHE_DIR/zulu8-arm64-jdk.zip" -d "$NEW_GAME_DIR/resources/runtime"
fi

find_java_tools
if [ -z "${JAVA_BIN:-}" ] || [ -z "${JAVAC_BIN:-}" ] || [ -z "${JAR_BIN:-}" ]; then
  echo "Could not find Java tools in the updated install."
  pause_if_tty
  exit 1
fi

say "Preserving local settings and replay files."
preserve_user_data "$GAME_DIR" "$NEW_GAME_DIR"
ensure_combo_goal_helper_setting "$NEW_GAME_DIR/settings"

say "Installing Apple Silicon native libraries."
LIB_ARM="$NEW_GAME_DIR/resources/libs-arm64"
rm -rf "$LIB_ARM"
mkdir -p "$LIB_ARM"
if [ -d "$NEW_GAME_DIR/resources/libs" ]; then
  cp -R "$NEW_GAME_DIR/resources/libs/." "$LIB_ARM/"
fi
mkdir -p "$WORK_DIR/lwjgl-platform"
unzip -q "$CACHE_DIR/lwjgl-platform-2.9.3-macos-aarch64.jar" -d "$WORK_DIR/lwjgl-platform"
copy_first "liblwjgl.dylib" "$LIB_ARM/liblwjgl.jnilib"
copy_first "libopenal.dylib" "$LIB_ARM/openal.dylib"
copy_first "libjcocoa.dylib" "$LIB_ARM/libjcocoa.dylib"
copy_first "libjinput-osx.dylib" "$LIB_ARM/libjinput-osx.jnilib"
copy_first "libjinput-osx.jnilib" "$LIB_ARM/libjinput-osx.jnilib"
ln -sf liblwjgl.jnilib "$LIB_ARM/liblwjgl64.jnilib"
ln -sf liblwjgl.jnilib "$LIB_ARM/liblwjgl64.dylib"

if [ ! -f "$LIB_ARM/liblwjgl.jnilib" ] || [ ! -f "$LIB_ARM/openal.dylib" ]; then
  echo "Could not extract the Apple Silicon LWJGL native libraries."
  pause_if_tty
  exit 1
fi
install_bass_music_support "$NEW_GAME_DIR" "$LIB_ARM" "$JAVA_HOME_DIR"

say "Reapplying Apple Silicon patches."
PATCHER_CLASSES="$WORK_DIR/patcher-classes"
PATCHED_CLASSES="$WORK_DIR/patched-classes"
mkdir -p "$PATCHER_CLASSES" "$PATCHED_CLASSES"
PATCHER_CP="$PATCHER_CLASSES:$CACHE_DIR/asm-9.7.1.jar:$CACHE_DIR/asm-commons-9.7.1.jar"

"$JAVAC_BIN" -source 1.8 -target 1.8 \
  -cp "$CACHE_DIR/asm-9.7.1.jar:$CACHE_DIR/asm-commons-9.7.1.jar" \
  -d "$PATCHER_CLASSES" \
  "$TOOLS_DIR/src/PatchJavaAudioEffects.java" \
  "$TOOLS_DIR/src/PatchClassVersion52.java" \
  "$TOOLS_DIR/src/PatchDisplayForceWindowed.java" \
  "$TOOLS_DIR/src/PatchMacOSXDisplaySafeResizable.java" \
  "$TOOLS_DIR/src/PatchDisplayStartupSettings.java" \
  "$TOOLS_DIR/src/PatchMusicVolumeSettingHook.java" \
  "$TOOLS_DIR/src/PatchLWJGLArmSupport.java" \
  "$TOOLS_DIR/src/RemapArmMacLWJGL.java"

"$JAVA_BIN" -cp "$PATCHER_CP" RemapArmMacLWJGL \
  "$CACHE_DIR/lwjgl-2.9.3-macos-aarch64.jar" \
  "$PATCHED_CLASSES"

extract_class "UE_281"
extract_class "org/lwjgl/E_681"
extract_class "org/lwjgl/Sys"
extract_class "org/lwjgl/input/K_701"
extract_class "org/lwjgl/opengl/Display"
extract_class "org/lwjgl/opengl/MacOSXDisplay"
extract_class "FE_76"
extract_class "JB_129"
extract_class "zy_1113"

patch_class PatchJavaAudioEffects "UE_281"
patch_class PatchLWJGLArmSupport "org/lwjgl/E_681"
patch_class PatchLWJGLArmSupport "org/lwjgl/Sys"
patch_class PatchLWJGLArmSupport "org/lwjgl/input/K_701"
patch_class PatchDisplayForceWindowed "org/lwjgl/opengl/Display"
patch_class PatchMacOSXDisplaySafeResizable "org/lwjgl/opengl/MacOSXDisplay"
patch_class PatchDisplayStartupSettings "FE_76"
patch_class PatchMusicVolumeSettingHook "JB_129"
patch_class PatchClassVersion52 "zy_1113"

if [ -f "$NEW_GAME_DIR/src/frontend/ColorPicker.java" ]; then
  perl -0pi -e 's/java\.nio\.file\.Files\.writeString\(Paths\.get\("settings\/background-color\.txt"\), colorString\);/java.nio.file.Files.write(Paths.get("settings\/background-color.txt"), colorString.getBytes(java.nio.charset.StandardCharsets.UTF_8));/g' \
    "$NEW_GAME_DIR/src/frontend/ColorPicker.java"
fi

{
  for source_file in \
    "$NEW_GAME_DIR/src/backend/BlockListManager.java" \
    "$NEW_GAME_DIR/src/backend/DisplayModeHelper.java" \
    "$NEW_GAME_DIR/src/backend/readanimtoggle.java" \
    "$NEW_GAME_DIR/src/backend/rugguUtils.java" \
    "$NEW_GAME_DIR/src/frontend/ColorPicker.java"; do
    if [ -f "$source_file" ]; then
      printf '%s\n' "$source_file"
    fi
  done
} > "$WORK_DIR/helper-sources.txt"
printf '%s\n' "$TOOLS_DIR/src/C2SettingsAppleSilicon.java" >> "$WORK_DIR/helper-sources.txt"
printf '%s\n' "$TOOLS_DIR/src/C2DisplaySettings.java" >> "$WORK_DIR/helper-sources.txt"
find "$TOOLS_DIR/src/java8compat" -name '*.java' -print >> "$WORK_DIR/helper-sources.txt"
printf '%s\n' "$TOOLS_DIR/src/java8stubs/zy_1113.java" >> "$WORK_DIR/helper-sources.txt"

"$JAVAC_BIN" -source 1.8 -target 1.8 -cp "$NEW_GAME_DIR/cultris2.jar" \
  -d "$PATCHED_CLASSES" @"$WORK_DIR/helper-sources.txt"
patch_class PatchClassVersion52 "zy_1113"

"$JAVAC_BIN" -source 1.8 -target 1.8 -d "$PATCHED_CLASSES" \
  "$TOOLS_DIR/src/ReadBackgroundColor.java" \
  "$TOOLS_DIR/src/C2JavaAudioEffects.java" \
  "$TOOLS_DIR/src/C2BassMusic.java"

"$JAR_BIN" uf "$NEW_GAME_DIR/cultris2.jar" -C "$PATCHED_CLASSES" .
zip -dq "$NEW_GAME_DIR/cultris2.jar" \
  'ColorPicker$1.class' \
  'c2settings$4.class' \
  'c2settings$5.class' \
  'c2settings$6.class' 2>/dev/null || true

say "Writing launchers and metadata."
mkdir -p "$NEW_GAME_DIR/launchers"
cat > "$NEW_GAME_DIR/launchers/macOS-cultris2.command" <<'EOF'
#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GAME_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JAVA_BIN="$(find "$GAME_DIR/resources/runtime" -path '*/Contents/Home/bin/java' -type f | head -n 1)"

if [ -z "$JAVA_BIN" ]; then
  echo "Could not find the bundled Apple Silicon Java runtime."
  read -r -p "Press Enter to close..."
  exit 1
fi

cd "$GAME_DIR"
RESIZE_SETTING="1"
if [ -f "$GAME_DIR/settings/disable-window-resize.txt" ]; then
  RESIZE_SETTING="$(head -n 1 "$GAME_DIR/settings/disable-window-resize.txt" | tr -d '[:space:]')"
fi

set +e
if [ "$RESIZE_SETTING" != "0" ]; then
  "$JAVA_BIN" \
    -Dc2.disableWindowResize=true \
    -Dapple.awt.application.name="Cultris II" \
    -Djava.library.path="$GAME_DIR/resources/libs-arm64" \
    -jar "$GAME_DIR/cultris2.jar"
else
  "$JAVA_BIN" \
    -Dapple.awt.application.name="Cultris II" \
    -Djava.library.path="$GAME_DIR/resources/libs-arm64" \
    -jar "$GAME_DIR/cultris2.jar"
fi
status=$?
set -e
if [ "$status" -ne 0 ]; then
  echo
  echo "Cultris II exited unexpectedly."
  echo "If it crashed while opening, resizing, or entering fullscreen, open C2 Settings.command"
  echo "and turn on Display -> Disable window resizing, then start the game again."
fi
exit "$status"
EOF

cat > "$NEW_GAME_DIR/launchers/macOS-c2settings.command" <<'EOF'
#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GAME_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JAVA_BIN="$(find "$GAME_DIR/resources/runtime" -path '*/Contents/Home/bin/java' -type f | head -n 1)"

if [ -z "$JAVA_BIN" ]; then
  echo "Could not find the bundled Apple Silicon Java runtime."
  read -r -p "Press Enter to close..."
  exit 1
fi

cd "$GAME_DIR"
exec "$JAVA_BIN" \
  -Dapple.awt.application.name="C2 Settings" \
  -Djava.library.path="$GAME_DIR/resources/libs-arm64" \
  -cp "$GAME_DIR/cultris2.jar" \
  c2settings
EOF

chmod +x \
  "$NEW_GAME_DIR/launchers/macOS-cultris2.command" \
  "$NEW_GAME_DIR/launchers/macOS-c2settings.command"
printf '%s\n' "$REMOTE_SHA" > "$NEW_GAME_DIR/.c2-upstream-stable-sha"

BACKUP_DIR="$INSTALL_ROOT/c2-patch Backup $(date +%Y%m%d-%H%M%S)"
say "Replacing installed files."
mv "$GAME_DIR" "$BACKUP_DIR"
mv "$NEW_GAME_DIR" "$GAME_DIR"

cat > "$INSTALL_ROOT/Play Cultris II.command" <<'EOF'
#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
C2_TERMINAL_TTY="$(tty 2>/dev/null || true)"
C2_TERMINAL_WINDOW_ID=""
if [ "${TERM_PROGRAM:-}" = "Apple_Terminal" ] && [ -n "$C2_TERMINAL_TTY" ] && [ "$C2_TERMINAL_TTY" != "not a tty" ]; then
  C2_TERMINAL_WINDOW_ID="$(osascript <<OSA 2>/dev/null || true
tell application "Terminal"
  repeat with w in windows
    repeat with t in tabs of w
      if tty of t is "$C2_TERMINAL_TTY" then
        return id of w
      end if
    end repeat
  end repeat
end tell
OSA
)"
fi

close_terminal_on_success() {
  if [ "${C2_KEEP_TERMINAL:-0}" = "1" ] || [ "${TERM_PROGRAM:-}" != "Apple_Terminal" ]; then
    return
  fi
  (
    sleep 1
    if [ -n "$C2_TERMINAL_WINDOW_ID" ]; then
      osascript <<OSA
tell application "Terminal"
  close (first window whose id is $C2_TERMINAL_WINDOW_ID)
end tell
OSA
    fi
  ) >/dev/null 2>&1 &
}

"$ROOT/c2-patch/launchers/macOS-cultris2.command"
status=$?
if [ "$status" -eq 0 ]; then
  close_terminal_on_success
fi
exit "$status"
EOF

cat > "$INSTALL_ROOT/C2 Settings.command" <<'EOF'
#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
C2_TERMINAL_TTY="$(tty 2>/dev/null || true)"
C2_TERMINAL_WINDOW_ID=""
if [ "${TERM_PROGRAM:-}" = "Apple_Terminal" ] && [ -n "$C2_TERMINAL_TTY" ] && [ "$C2_TERMINAL_TTY" != "not a tty" ]; then
  C2_TERMINAL_WINDOW_ID="$(osascript <<OSA 2>/dev/null || true
tell application "Terminal"
  repeat with w in windows
    repeat with t in tabs of w
      if tty of t is "$C2_TERMINAL_TTY" then
        return id of w
      end if
    end repeat
  end repeat
end tell
OSA
)"
fi

close_terminal_on_success() {
  if [ "${C2_KEEP_TERMINAL:-0}" = "1" ] || [ "${TERM_PROGRAM:-}" != "Apple_Terminal" ]; then
    return
  fi
  (
    sleep 1
    if [ -n "$C2_TERMINAL_WINDOW_ID" ]; then
      osascript <<OSA
tell application "Terminal"
  close (first window whose id is $C2_TERMINAL_WINDOW_ID)
end tell
OSA
    fi
  ) >/dev/null 2>&1 &
}

"$ROOT/c2-patch/launchers/macOS-c2settings.command"
status=$?
if [ "$status" -eq 0 ]; then
  close_terminal_on_success
fi
exit "$status"
EOF

SUPPORT_INSTALL_DIR="$INSTALL_ROOT/.c2-apple-silicon-installer"
rm -rf "$SUPPORT_INSTALL_DIR"
mkdir -p "$SUPPORT_INSTALL_DIR"
cp -R "$TOOLS_STAGING_DIR/tools" "$SUPPORT_INSTALL_DIR/tools"
printf '%s\n' "$ACTIVE_INSTALLER_VERSION" > "$SUPPORT_INSTALL_DIR/version"
if [ -f "$ACTIVE_UPDATER_SCRIPT" ]; then
  cp "$ACTIVE_UPDATER_SCRIPT" "$INSTALL_ROOT/Update Cultris II.command"
fi
chmod +x \
  "$INSTALL_ROOT/Play Cultris II.command" \
  "$INSTALL_ROOT/C2 Settings.command" \
  "$INSTALL_ROOT/Update Cultris II.command"

echo
echo "Update complete."
echo "Previous install was moved to:"
echo "$BACKUP_DIR"
echo
echo "Double-click Play Cultris II.command to start the game."
open "$INSTALL_ROOT"
pause_if_tty
