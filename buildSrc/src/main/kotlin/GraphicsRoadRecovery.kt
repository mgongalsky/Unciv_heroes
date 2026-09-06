package com.unciv.build

import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/** One-time recovery of six legacy road regions absent from the Kitchen source tree. */
object GraphicsRoadRecovery {
    private data class Region(
        val page: String,
        val fields: MutableMap<String, String> = linkedMapOf()
    )

    fun recover(projectRoot: File) {
        val assets = File(projectRoot, "android/assets").canonicalFile
        val atlas = File(assets, "Tilesets.atlas")
        require(atlas.isFile) { "Missing working atlas: $atlas" }
        val names = listOf("000001", "000010", "000100", "001000", "010000", "100000")
            .map { "TileSets/Roads/hexRoad-$it-003" }
        val regions = linkedMapOf<String, Region>()
        var page: String? = null
        var current: Region? = null
        var nextIsPage = true
        atlas.forEachLine { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> {
                    nextIsPage = true; current = null
                }

                nextIsPage -> {
                    require(
                        line.endsWith(
                            ".png",
                            true
                        ) && '/' !in line && '\\' !in line && ':' !in line
                    ) {
                        "Unsupported atlas page: $line"
                    }
                    page = line
                    current = null
                    nextIsPage = false
                }

                ':' in line -> {
                    current?.fields?.set(
                        line.substringBefore(':').trim(),
                        line.substringAfter(':').trim()
                    )
                }

                else -> {
                    val region = Region(requireNotNull(page))
                    if (line in names) {
                        require(!regions.containsKey(line)) { "Duplicate road region: $line" }
                        regions[line] = region
                    }
                    current = region
                }
            }
        }
        require(regions.keys.containsAll(names)) { "Working atlas lacks roads: ${names - regions.keys}" }
        val base = File(projectRoot, "graphics/base/Tilesets").canonicalFile
        require(base.isDirectory) { "Import Kitchen sources first: $base" }
        val recovered = linkedMapOf<File, BufferedImage>()
        for (name in names) {
            val region = regions.getValue(name)
            fun pair(key: String): List<Int> {
                val values = requireNotNull(region.fields[key]) { "Missing $key for $name" }
                    .split(',').map { it.trim().toInt() }
                require(values.size == 2) { "Invalid $key for $name" }
                return values
            }
            require(region.fields["rotate"] == "false") { "Rotated road recovery is unsupported: $name" }
            require(region.fields["index"] == "-1") { "Indexed road recovery is unsupported: $name" }
            val xy = pair("xy")
            val size = pair("size")
            require(pair("orig") == size && pair("offset") == listOf(0, 0)) {
                "Trimmed road recovery is unsupported: $name"
            }
            val pageFile = File(assets, region.page).canonicalFile
            require(pageFile.parentFile == assets) { "Unsafe page path: $pageFile" }
            val image = requireNotNull(ImageIO.read(pageFile)) { "Cannot decode $pageFile" }
            try {
                require(xy.all { it >= 0 } && size.all { it > 0 } &&
                        xy[0].toLong() + size[0] <= image.width && xy[1].toLong() + size[1] <= image.height) {
                    "Road region outside PNG page: $name"
                }
                val output = BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB)
                output.setRGB(
                    0, 0, size[0], size[1],
                    image.getRGB(xy[0], xy[1], size[0], size[1], null, 0, size[0]), 0, size[0]
                )
                val target = File(base, "$name.png")
                require(
                    target.canonicalFile.toPath().startsWith(base.toPath())
                ) { "Unsafe output: $target" }
                if (target.exists()) {
                    val existing =
                        requireNotNull(ImageIO.read(target)) { "Cannot decode existing $target" }
                    try {
                        require(
                            existing.width == output.width && existing.height == output.height &&
                                    existing.getRGB(
                                        0,
                                        0,
                                        existing.width,
                                        existing.height,
                                        null,
                                        0,
                                        existing.width
                                    )
                                        .contentEquals(
                                            output.getRGB(
                                                0,
                                                0,
                                                output.width,
                                                output.height,
                                                null,
                                                0,
                                                output.width
                                            )
                                        )
                        ) {
                            "Existing road differs; refusing to overwrite: $target"
                        }
                    } finally {
                        existing.flush()
                    }
                    output.flush()
                } else recovered[target] = output
            } finally {
                image.flush()
            }
        }
        for ((target, image) in recovered) {
            try {
                require(target.parentFile.isDirectory || target.parentFile.mkdirs()) { "Cannot create ${target.parentFile}" }
                Files.newOutputStream(
                    target.toPath(), java.nio.file.StandardOpenOption.CREATE_NEW,
                    java.nio.file.StandardOpenOption.WRITE
                ).use { stream ->
                    check(ImageIO.write(image, "png", stream)) { "PNG writer unavailable" }
                }
                println("Recovered road source: ${target.relativeTo(projectRoot)}")
            } finally {
                image.flush()
            }
        }
        println("Road recovery complete: ${recovered.size} created; ${names.size - recovered.size} already matched.")
    }
}
