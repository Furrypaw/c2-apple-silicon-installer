# Cultris II for Apple Silicon Macs

Double-click `Install Cultris II for Apple Silicon.command`.

The installer creates a `C2` folder on your Desktop with:

- `Play Cultris II.command`
- `C2 Settings.command`
- the original c2-patch download
- Apple Silicon Java 8
- Apple Silicon LWJGL native libraries
- local patches needed for macOS arm64

It does not include the Cultris II game jar or Java runtime in this repository. Those are downloaded during install.

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
