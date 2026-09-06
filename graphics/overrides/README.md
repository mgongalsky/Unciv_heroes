# Sprite overrides

Place new or replacement sprites here, preserving their atlas-relative paths:

- `Tilesets/TileSets/HexaRealm/Tiles/Grassland.png`
- `AbsoluteUnits/TileSets/AbsoluteUnits/Units/Swordsman.png`

`previewImages` merges `graphics/base/<atlas>` with `graphics/overrides/<atlas>`.
An override replaces the base file at the same relative path; a new path adds a
sprite. Removing an override restores the base sprite on the next preview.
Use the same filename, extension and letter case as the base sprite being replaced.

Run `:android:previewImages`, then `:android:publishImages -Ppreview=<preview-id>`.
Restart the game to inspect the result. Run `:android:backupImages` to save an
accepted version. See `wiki/Make-sprite-atlas.md` for the full workflow.
