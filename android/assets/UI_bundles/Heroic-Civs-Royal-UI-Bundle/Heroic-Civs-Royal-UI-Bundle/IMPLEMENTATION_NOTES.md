# Notes for the implementing model

## Authoritative choice

Use `selection.json`. Do not infer a different style from the experimental assets retained in `reference-web`.
The chosen display font is Berkshire Swash, the body/number font is EB Garamond; type-scale is 1.25. UI copy is English.

## Images and slicing

`assets/ui/` is the import-friendly PNG set. `assets-manifest.json` maps every PNG to its original source and records its dimensions/hash. Original WebP files remain in `reference-web/assets/` as a reference.

Preserve alpha; do not stretch complete window frames as one image. Preserve the corner regions and stretch the undecorated straight rails. Grand, compact and minimal are separate artworks. The compact Royal border is the **repaired v2** artwork: its source corners use **108px on each side**. The older 22% compact slice cuts the leaves and must not be used for that border.

The numeric rules are in `rendering.json`; the browser implementation is in `reference-web/style.css`, `assets/frame-metrics.css`, `assets/royal-frame-repair.css` and the tab stylesheets. These are CSS source-slice rules, not pre-authored engine-specific nine-patch files. Convert them to whatever atlas/drawable representation the game uses.

Grand and compact decision-window crests align with their rail at the same artwork scale; do not place the crown on an unrelated floating strip.

## Navigation: one continuous surface

Scribal ribbon uses the Royal scroll skin, with `saturate(.65)` on its idle skin. It is one object with common rails/end rolls and thin internal separators. There are six target regions, not six individual action buttons.

For the selected area, draw the **same complete skin in register**, apply the `active-royal` colour matrix in `assets/ui/navigation/selection-palettes.svg`, and reveal only the selected region. Repaint the matching common rail image above it. This keeps grain/ornament positions continuous. Baking an equivalent tinted source skin is also fine if useful for the game's renderer.

Quiet corners adds just two small 17px leaf/brace inlays, upper-left and lower-right (the latter rotated 180 degrees). The selected label is pale cream. Do not use the rejected Banded sides/button-leather insert. The original button decoration belongs to action buttons.

The reference uses a 76px-high ribbon, 45px end regions, six equal sections inside those ends and 2px feathering at the selection boundary. These values are design-space reference measurements; adapt the overall scale for the game's UI. On narrow screens the original ribbon scrolls horizontally rather than splitting the artwork.

## Buttons and other controls

`button.png` is a three-slice horizontal plaque; side source slices are 16% and rendered end regions 25px in the reference. `face.png` supplies material for small square controls. Hover, pressed, secondary and disabled are colour/offset states, not separate button images. Their values are recorded in rendering.json and the CSS.

Checkboxes, radios, sliders, bars, paper fills and other basic geometry are drawn with CSS in the original, using the supplied icons, colours and textures. There is no hidden bitmap pack for those controls. Recreate their geometry in the game using the live reference and screenshots.

## Fonts and notices

Icons are provided both as standalone SVG and as white transparent 64×64 PNG for tinting.

The original, unmodified font files are provided with each family's copyright and SIL Open Font License 1.1. Keep those notices with redistributed fonts.
Berkshire Swash is the supplied regular face; the browser may synthesize heavier text for headings. EB Garamond is a variable font (axes are in assets/fonts/catalog.json). If the game pipeline needs static/baked fonts, prepare them from these originals while respecting the accompanying OFL and reserved-name terms. Glyph coverage depends on the actual files; the demonstrated UI is English.

## Screenshots and source

`01-controls.jpg` shows controls, indicator states, selected tab and small/large frames; `02-dialog.jpg` shows a dialog. Both use the chosen fonts and 125% text setting.
The reference contains illustrative game data and old alternate material directions. They are not gameplay requirements. No credentials, repository metadata or hosting access tokens are included.
