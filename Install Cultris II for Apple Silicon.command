#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_ROOT="${C2_INSTALL_ROOT:-$HOME/Desktop/C2}"
CACHE_DIR="$SCRIPT_DIR/.cache"
WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/c2-arm-install.XXXXXX")"

C2_PATCH_URL="${C2_PATCH_URL:-https://github.com/shayklos/c2-patch/archive/refs/heads/stable.zip}"
C2_PATCH_BRANCH_API_URL="${C2_PATCH_BRANCH_API_URL:-https://api.github.com/repos/shayklos/c2-patch/branches/stable}"
C2_PATCH_BACKUP_URL="${C2_PATCH_BACKUP_URL:-https://data.catgc.com/c2-patch-stable%20%2821.06.26%29.zip}"
C2_PATCH_JAR_BACKUP_URL="${C2_PATCH_JAR_BACKUP_URL:-https://data.catgc.com/cultris2.jar}"
ZULU_JDK_URL="${ZULU_JDK_URL:-https://cdn.azul.com/zulu/bin/zulu8.94.0.17-ca-jdk8.0.492-macosx_aarch64.zip}"
ASM_URL="${ASM_URL:-https://repo1.maven.org/maven2/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar}"
ASM_COMMONS_URL="${ASM_COMMONS_URL:-https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/9.7.1/asm-commons-9.7.1.jar}"
LWJGL_JAR_URL="${LWJGL_JAR_URL:-https://files.betacraft.uk/launcher/v2/assets/libraries/lwjgl/lwjgl-2.9.3-macos-aarch64.jar}"
LWJGL_PLATFORM_URL="${LWJGL_PLATFORM_URL:-https://files.betacraft.uk/launcher/v2/assets/libraries/lwjgl/lwjgl-platform-2.9.3-macos-aarch64.jar}"

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

zip_comment_sha() {
  unzip -z "$1" 2>/dev/null \
    | sed -n 's/^\([0-9a-f]\{40\}\)$/\1/p' \
    | head -n 1
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

extract_class() {
  local class_path="$1"
  local out="$WORK_DIR/original-classes/$class_path.class"
  mkdir -p "$(dirname "$out")"
  unzip -p "$GAME_DIR/cultris2.jar" "$class_path.class" > "$out"
}

patch_class() {
  local patcher="$1"
  local class_path="$2"
  local in="$WORK_DIR/original-classes/$class_path.class"
  local out="$PATCHED_CLASSES/$class_path.class"
  mkdir -p "$(dirname "$out")"
  "$JAVA_BIN" -cp "$PATCHER_CP" "$patcher" "$in" "$out"
}

if [ "$(uname -m)" != "arm64" ]; then
  echo "This installer is for Apple Silicon Macs."
  echo "This Mac reports: $(uname -m)"
  pause_if_tty
  exit 1
fi

mkdir -p "$CACHE_DIR"

say "Step 1/6: downloading the game patch, Java, and Apple Silicon graphics libraries."
if ! download "$C2_PATCH_URL" "$CACHE_DIR/c2-patch-stable.zip"; then
  if [ -n "$C2_PATCH_BACKUP_URL" ]; then
    echo "Primary c2-patch download failed. Trying backup URL."
    download "$C2_PATCH_BACKUP_URL" "$CACHE_DIR/c2-patch-stable.zip"
  else
    echo "Primary c2-patch download failed and no C2_PATCH_BACKUP_URL is set."
    pause_if_tty
    exit 1
  fi
fi
download "$ZULU_JDK_URL" "$CACHE_DIR/zulu8-arm64-jdk.zip"
download "$ASM_URL" "$CACHE_DIR/asm-9.7.1.jar"
download "$ASM_COMMONS_URL" "$CACHE_DIR/asm-commons-9.7.1.jar"
download "$LWJGL_JAR_URL" "$CACHE_DIR/lwjgl-2.9.3-macos-aarch64.jar"
download "$LWJGL_PLATFORM_URL" "$CACHE_DIR/lwjgl-platform-2.9.3-macos-aarch64.jar"

say "Step 2/6: preparing the Desktop C2 folder."
if [ -e "$INSTALL_ROOT" ]; then
  BACKUP="$INSTALL_ROOT Backup $(date +%Y%m%d-%H%M%S)"
  echo "Existing C2 folder found. Moving it to:"
  echo "$BACKUP"
  mv "$INSTALL_ROOT" "$BACKUP"
fi
mkdir -p "$INSTALL_ROOT"
unzip -q "$CACHE_DIR/c2-patch-stable.zip" -d "$WORK_DIR"
GAME_DIR="$INSTALL_ROOT/c2-patch"
cp -R "$WORK_DIR/c2-patch-stable" "$GAME_DIR"
UPSTREAM_SHA="$(latest_upstream_sha || true)"
if [ -z "$UPSTREAM_SHA" ]; then
  UPSTREAM_SHA="$(zip_comment_sha "$CACHE_DIR/c2-patch-stable.zip" || true)"
fi

say "Step 3/6: installing Apple Silicon Java."
mkdir -p "$GAME_DIR/resources/runtime"
unzip -q "$CACHE_DIR/zulu8-arm64-jdk.zip" -d "$GAME_DIR/resources/runtime"
JAVA_BIN="$(find "$GAME_DIR/resources/runtime" -path '*/Contents/Home/bin/java' -type f | head -n 1)"
JAVAC_BIN="$(find "$GAME_DIR/resources/runtime" -path '*/Contents/Home/bin/javac' -type f | head -n 1)"
JAR_BIN="$(find "$GAME_DIR/resources/runtime" -path '*/Contents/Home/bin/jar' -type f | head -n 1)"
if [ -z "$JAVA_BIN" ] || [ -z "$JAVAC_BIN" ] || [ -z "$JAR_BIN" ]; then
  echo "Could not find Java tools in the downloaded Zulu JDK."
  pause_if_tty
  exit 1
fi
JAVA_HOME_DIR="$(cd "$(dirname "$JAVA_BIN")/.." && pwd)"

say "Step 4/6: installing native Apple Silicon LWJGL libraries."
LIB_ARM="$GAME_DIR/resources/libs-arm64"
rm -rf "$LIB_ARM"
mkdir -p "$LIB_ARM"
if [ -d "$GAME_DIR/resources/libs" ]; then
  cp -R "$GAME_DIR/resources/libs/." "$LIB_ARM/"
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

say "Step 5/6: patching Cultris II for Apple Silicon."
PATCHER_CLASSES="$WORK_DIR/patcher-classes"
PATCHED_CLASSES="$WORK_DIR/patched-classes"
mkdir -p "$PATCHER_CLASSES" "$PATCHED_CLASSES"
PATCHER_CP="$PATCHER_CLASSES:$CACHE_DIR/asm-9.7.1.jar:$CACHE_DIR/asm-commons-9.7.1.jar"

"$JAVAC_BIN" -source 1.8 -target 1.8 \
  -cp "$CACHE_DIR/asm-9.7.1.jar:$CACHE_DIR/asm-commons-9.7.1.jar" \
  -d "$PATCHER_CLASSES" \
  "$SCRIPT_DIR/tools/src/PatchJavaAudioEffects.java" \
  "$SCRIPT_DIR/tools/src/PatchClassVersion52.java" \
  "$SCRIPT_DIR/tools/src/PatchDisplayForceWindowed.java" \
  "$SCRIPT_DIR/tools/src/PatchLWJGLArmSupport.java" \
  "$SCRIPT_DIR/tools/src/RemapArmMacLWJGL.java"

"$JAVA_BIN" -cp "$PATCHER_CP" RemapArmMacLWJGL \
  "$CACHE_DIR/lwjgl-2.9.3-macos-aarch64.jar" \
  "$PATCHED_CLASSES"

extract_class "UE_281"
extract_class "org/lwjgl/E_681"
extract_class "org/lwjgl/Sys"
extract_class "org/lwjgl/input/K_701"
extract_class "org/lwjgl/opengl/Display"
extract_class "zy_1113"

patch_class PatchJavaAudioEffects "UE_281"
patch_class PatchLWJGLArmSupport "org/lwjgl/E_681"
patch_class PatchLWJGLArmSupport "org/lwjgl/Sys"
patch_class PatchLWJGLArmSupport "org/lwjgl/input/K_701"
patch_class PatchDisplayForceWindowed "org/lwjgl/opengl/Display"
patch_class PatchClassVersion52 "zy_1113"

if [ -f "$GAME_DIR/src/frontend/ColorPicker.java" ]; then
  perl -0pi -e 's/java\.nio\.file\.Files\.writeString\(Paths\.get\("settings\/background-color\.txt"\), colorString\);/java.nio.file.Files.write(Paths.get("settings\/background-color.txt"), colorString.getBytes(java.nio.charset.StandardCharsets.UTF_8));/g' \
    "$GAME_DIR/src/frontend/ColorPicker.java"
fi

{
  for source_file in \
    "$GAME_DIR/src/backend/BlockListManager.java" \
    "$GAME_DIR/src/backend/DisplayModeHelper.java" \
    "$GAME_DIR/src/backend/readanimtoggle.java" \
    "$GAME_DIR/src/backend/rugguUtils.java" \
    "$GAME_DIR/src/frontend/ColorPicker.java" \
    "$GAME_DIR/src/frontend/c2settings.java"; do
    if [ -f "$source_file" ]; then
      printf '%s\n' "$source_file"
    fi
  done
} > "$WORK_DIR/helper-sources.txt"
find "$SCRIPT_DIR/tools/src/java8compat" -name '*.java' -print >> "$WORK_DIR/helper-sources.txt"
printf '%s\n' "$SCRIPT_DIR/tools/src/java8stubs/zy_1113.java" >> "$WORK_DIR/helper-sources.txt"

"$JAVAC_BIN" -source 1.8 -target 1.8 -cp "$GAME_DIR/cultris2.jar" \
  -d "$PATCHED_CLASSES" @"$WORK_DIR/helper-sources.txt"
patch_class PatchClassVersion52 "zy_1113"

"$JAVAC_BIN" -source 1.8 -target 1.8 -d "$PATCHED_CLASSES" \
  "$SCRIPT_DIR/tools/src/ReadBackgroundColor.java" \
  "$SCRIPT_DIR/tools/src/C2JavaAudioEffects.java"

"$JAR_BIN" uf "$GAME_DIR/cultris2.jar" -C "$PATCHED_CLASSES" .
zip -dq "$GAME_DIR/cultris2.jar" \
  'ColorPicker$1.class' \
  'c2settings$4.class' \
  'c2settings$5.class' \
  'c2settings$6.class' 2>/dev/null || true

say "Step 6/6: creating double-click launchers."
mkdir -p "$GAME_DIR/launchers"
cat > "$GAME_DIR/launchers/macOS-cultris2.command" <<'EOF'
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
  -Dapple.awt.application.name="Cultris II" \
  -Djava.library.path="$GAME_DIR/resources/libs-arm64" \
  -jar "$GAME_DIR/cultris2.jar"
EOF

cat > "$GAME_DIR/launchers/macOS-c2settings.command" <<'EOF'
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

cat > "$INSTALL_ROOT/Play Cultris II.command" <<'EOF'
#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$ROOT/c2-patch/launchers/macOS-cultris2.command"
EOF

cat > "$INSTALL_ROOT/C2 Settings.command" <<'EOF'
#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$ROOT/c2-patch/launchers/macOS-c2settings.command"
EOF

chmod +x \
  "$GAME_DIR/launchers/macOS-cultris2.command" \
  "$GAME_DIR/launchers/macOS-c2settings.command" \
  "$INSTALL_ROOT/Play Cultris II.command" \
  "$INSTALL_ROOT/C2 Settings.command"

SUPPORT_DIR="$INSTALL_ROOT/.c2-apple-silicon-installer"
rm -rf "$SUPPORT_DIR"
mkdir -p "$SUPPORT_DIR"
cp -R "$SCRIPT_DIR/tools" "$SUPPORT_DIR/tools"
if [ -f "$SCRIPT_DIR/Update Cultris II for Apple Silicon.command" ]; then
  cp "$SCRIPT_DIR/Update Cultris II for Apple Silicon.command" "$INSTALL_ROOT/Update Cultris II.command"
  chmod +x "$INSTALL_ROOT/Update Cultris II.command"
fi
printf '%s\n' "${UPSTREAM_SHA:-unknown}" > "$GAME_DIR/.c2-upstream-stable-sha"

cat > "$GAME_DIR/README-macOS-Apple-Silicon.txt" <<EOF
Cultris II Apple Silicon setup

Open "$INSTALL_ROOT/Play Cultris II.command" to play.
Open "$INSTALL_ROOT/C2 Settings.command" for settings.

Background color:
Edit "$GAME_DIR/settings/background-color.txt"
Use normal RGB values like:
47, 47, 47
Restart the game after changing the file.

Note:
Native BASS audio is disabled on Apple Silicon because the old BASS library is not arm64.
EOF

echo
echo "Done. Cultris II is installed here:"
echo "$INSTALL_ROOT"
echo
echo "Double-click Play Cultris II.command to start the game."
open "$INSTALL_ROOT"
pause_if_tty
