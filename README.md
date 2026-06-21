# Cultris II for Apple Silicon Macs

Download the latest release ZIP, unzip it, then double-click:

`Install Cultris II for Apple Silicon.command`

Do not download only the `.command` file from the source browser. The installer also needs the `tools/` folder that is included in the release ZIP.

The installer creates a `C2` folder on your Desktop with:

- `Play Cultris II.command`
- `C2 Settings.command`
- the original c2-patch download
- Apple Silicon Java 8
- Apple Silicon LWJGL native libraries
- local patches needed for macOS arm64

It does not include the Cultris II game jar or Java runtime in this repository. Those are downloaded during install.

## Where the Game Comes From

The installer downloads the original `shayklos/c2-patch` stable ZIP, extracts `cultris2.jar`, then patches that jar locally on your Mac for Apple Silicon.

By default:

`https://github.com/shayklos/c2-patch/archive/refs/heads/stable.zip`

If that upstream URL disappears and you have a permitted/private mirror of the same ZIP, you can run:

```bash
C2_PATCH_BACKUP_URL="https://example.com/c2-patch-stable.zip" ./Install\ Cultris\ II\ for\ Apple\ Silicon.command
```

This repository intentionally does not publicly mirror `cultris2.jar` or the full c2-patch ZIP because the game binary does not appear to have a clear redistribution license.

## Background Color

After installing, edit:

`Desktop/C2/c2-patch/settings/background-color.txt`

Use normal RGB values:

```text
47, 47, 47
```

Restart the game after changing the file.

## Audio

The old native BASS audio library is disabled on Apple Silicon because the bundled BASS binary is not arm64. The game should run natively without Rosetta or VM emulation, but it will be silent unless a future arm64-compatible audio patch is added.
