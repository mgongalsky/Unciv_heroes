1. Take Images.Tileset folder from unciv/android.
2. Change hexes you want with keeping same names. 
Terrain hexes are in Images.Tileset/TileSets/HexaRealm/Tiles
You can add more variants for same hexes: Grassland4.png for example. They will be treated automatically.
2a. Resize your hexes if necessary. The required hexagons must be right. Use [ImageMagick](https://imagemagick.org/index.php)for it:
`"C:\Program Files\ImageMagick-7.1.0-Q16-HDRI\magick.exe" mogrify -resize 100%x116% *.png`
3. Make pack.json file inside Images.Tileset directory:
`{
	maxWidth: 2048,
	maxHeight: 2048,
	combineSubdirectories: true,
}`
4. Make altas via [Texture Packer](https://libgdx.com/wiki/tools/texture-packer).
The typical command is (make sure Images.Tileset folder is avaliable from your current path):
`java -cp runnable-texturepacker.jar com.badlogic.gdx.tools.texturepacker.TexturePacker Images.Tilesets Images.Tilesets-packed Tilesets`
5. You will have altas pngs and altas file in a folder Images.Tileset-packed
6. Copy everything from Images.Tileset-packed into unciv/android/assets
7. Recompile your game
