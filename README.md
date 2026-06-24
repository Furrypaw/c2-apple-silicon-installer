# Cultris II for Apple Silicon Macs

Download the latest release ZIP, unzip it, then double-click:

`Install Cultris II for Apple Silicon.command`

Do not download only the `.command` file from the source browser. The installer also needs the `tools/` folder that is included in the release ZIP.

## macOS Malware Warning

If macOS says:

`Apple could not verify "Install Cultris II for Apple Silicon.command" is free of malware that may harm your Mac or compromise your privacy.`

Use one of these options.

### Option 1: Right-click Open

1. Open the unzipped `c2-apple-silicon-installer` folder.
2. Hold `Control` and click `Install Cultris II for Apple Silicon.command`.
3. Choose `Open`.
4. Click `Open` again if macOS asks for confirmation.

### Option 2: System Settings

1. Try opening `Install Cultris II for Apple Silicon.command` once.
2. Open `System Settings`.
3. Go to `Privacy & Security`.
4. Scroll down and click `Open Anyway` for the blocked installer.
5. Try opening the installer again.

### Option 3: Terminal

Use this only for the ZIP downloaded from this repository's release page:

```bash
xattr -dr com.apple.quarantine ~/Downloads/c2-apple-silicon-installer
```

If you unzipped it somewhere else, replace the path with that folder's path. Then double-click `Install Cultris II for Apple Silicon.command` again.

The installer creates a `C2` folder on your Desktop with:

- `Play Cultris II.command`
- `C2 Settings.command`
- `Update Cultris II.command`
- the original c2-patch download
- Apple Silicon Java 8
- Apple Silicon LWJGL native libraries
- local patches needed for macOS arm64

It does not include the Cultris II game jar or Java runtime in this repository. Those are downloaded during install.

Use `C2 Settings.command` to change common options without editing text files.

## Resize Warning

Window resizing and fullscreen are experimental on Apple Silicon and are disabled by default for stability. Cultris II uses old LWJGL 2 OpenGL code, and macOS now runs OpenGL through Apple's Metal-backed compatibility layer. To try resize/fullscreen, open `C2 Settings.command`, turn **Display -> Disable window resizing** off, then restart the game. If the game crashes while opening, resizing, or entering fullscreen, turn that setting back on.

## Where the Game Comes From

The installer downloads the original `shayklos/c2-patch` stable ZIP, extracts `cultris2.jar`, then patches that jar locally on your Mac for Apple Silicon.

By default:

`https://github.com/shayklos/c2-patch/archive/refs/heads/stable.zip`

If that upstream URL disappears, the installer automatically falls back to this mirror:

`https://data.catgc.com/c2-patch-stable%20%2821.06.26%29.zip`

There is also a direct mirrored jar here for emergency/manual recovery:

`https://data.catgc.com/cultris2.jar`

You can override the full ZIP mirror if needed:

```bash
C2_PATCH_BACKUP_URL="https://example.com/c2-patch-stable.zip" ./Install\ Cultris\ II\ for\ Apple\ Silicon.command
```

## Updating

After installing, double-click:

`Desktop/C2/Update Cultris II.command`

The updater checks the latest `stable` branch commit on `shayklos/c2-patch` and compares it with the commit recorded in your local install. If upstream changed, it downloads the new c2-patch ZIP, keeps your local `settings/` files and replay files, reapplies the Apple Silicon patches, and moves the previous install to a timestamped backup folder.

The updater also checks the latest release of this Apple Silicon installer repo. If newer patch tools or scripts are available here, it downloads that release ZIP, uses the newer tools for the rebuild, and refreshes `Desktop/C2/Update Cultris II.command` plus the hidden support files. This is how fixes like new settings UI controls can reach an existing install even when upstream `c2-patch` did not change.

If you want to update even when the recorded commit matches:

```bash
C2_FORCE_UPDATE=1 ~/Desktop/C2/Update\ Cultris\ II.command
```

## Audio

Short game sound effects work through Java's native arm64 audio path, so they do not require Rosetta or VM emulation.

Music uses Cultris II's original MO3 tracker files through an arm64-native BASS bridge. The in-game music volume slider is hooked into that bridge, so music volume changes no longer require restarting the game. The installer downloads the official macOS BASS package from Un4seen, extracts the arm64 `libbass.dylib`, and builds a tiny JNI bridge locally.

BASS is downloaded from:

`https://www.un4seen.com/files/bass24-osx.zip`

Un4seen says BASS is free for non-commercial use. Commercial usage may require a BASS license.
