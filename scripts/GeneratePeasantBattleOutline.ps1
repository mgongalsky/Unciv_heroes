$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
        Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System . Drawing;
using System . Drawing . Imaging;

public static class PeasantBattleOutlineGenerator {
    public static void Generate(string sourcePath, string destinationPath)
    {
        using(var source = new Bitmap(sourcePath))
        using(var output = new Bitmap(source.Width, source.Height, PixelFormat.Format32bppArgb)) {
        int width = source . Width;
        int height = source . Height;
        var alpha = new byte [width * height];
        for (int y = 0; y < height; y++)
        for (int x = 0; x < width; x++)
        alpha[y * width + x] = source.GetPixel(x, y).A;

        // Six source pixels become a 1.5-pixel outline at the battle sprite scale.
        const int radius = 6;
        int visiblePixels = 0;
        for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
        int expanded = 0;
        for (int dy = -radius; dy <= radius; dy++) {
        int sy = y +dy;
        if (sy < 0 || sy >= height) continue;
        for (int dx = -radius; dx <= radius; dx++) {
        if (dx * dx + dy * dy > radius * radius) continue;
        int sx = x +dx;
        if (sx < 0 || sx >= width) continue;
        expanded = Math.Max(expanded, alpha[sy * width + sx]);
    }
    }
        int outlineAlpha = Math . Max (0, expanded-alpha[y * width+x]);
        output.SetPixel(x, y, Color.FromArgb(outlineAlpha, 255, 255, 255));
        if (outlineAlpha > 0) visiblePixels++;
    }
    }
        if (visiblePixels == 0) throw new InvalidOperationException ("Generated outline is empty");
        output.Save(destinationPath, ImageFormat.Png);
        Console.WriteLine(
            "Created Peasant outline: {0}x{1}, {2} visible pixels",
            width,
            height,
            visiblePixels
        );
    }
    }
}
'@
$projectDirectory = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $projectDirectory 'graphics/base/AbsoluteUnits/TileSets/AbsoluteUnits/Units/Peasant.png'
$destinationPath = Join-Path $projectDirectory 'android/assets/ExtraImages/BattleOutline-Peasant.png'
if (Test-Path -LiteralPath $destinationPath) {
    throw "Outline already exists; review it before regenerating: $destinationPath"
}
[PeasantBattleOutlineGenerator]::Generate($sourcePath, $destinationPath)
