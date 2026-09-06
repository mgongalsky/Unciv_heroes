package com.example.devtools.units

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.badlogic.gdx.utils.Json
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Locale
import javax.imageio.ImageIO

/** Packs captured source images without modifying project sources or installed atlases. */
object UnitAtlasPreparation {
    data class ImageEdit(
        val kind: UnitImages.Kind,
        val target: Path,
        val expected: ByteArray?,
        val bytes: ByteArray
    )

    class Prepared(
        val changes: List<UnitFileTransaction.Change>,
        val atlasNames: Set<String>,
        private val verifyInputs: () -> Unit
    ) {
        fun verifyUnchanged() = verifyInputs()
    }

    private data class AtlasInfo(val pages: Set<Path>, val regions: Set<String>)

    private fun read(path: Path): ByteArray? =
        if (Files.exists(path)) Files.readAllBytes(path) else null

    private fun same(first: ByteArray?, second: ByteArray?): Boolean =
        if (first == null) second == null else second != null && first.contentEquals(second)

    private fun hash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun sources(root: Path): Map<String, Path> {
        if (!Files.exists(root)) return emptyMap()
        require(Files.isDirectory(root)) { "Нет папки исходников: $root" }
        val canonical = root.toFile().canonicalFile.toPath()
        val result = sortedMapOf<String, Path>()
        val stream = Files.walk(root)
        try {
            stream.forEach { path ->
                require(
                    !Files.isSymbolicLink(path) && path.toFile().canonicalFile.toPath()
                        .startsWith(canonical)
                ) {
                    "Недопустимая ссылка в исходниках: $path"
                }
                if (Files.isRegularFile(path)) {
                    val name = path.fileName.toString()
                    if (name.substringAfterLast('.').lowercase(Locale.ROOT) in setOf(
                            "png",
                            "jpg",
                            "jpeg"
                        ) ||
                        name == "pack.json" || name == "TexturePacker.settings"
                    ) {
                        result[root.relativize(path).toString().replace('\\', '/')] = path
                    }
                }
            }
        } finally {
            stream.close()
        }
        return result
    }

    private fun atlasFiles(assets: Path): List<Path> {
        val stream = Files.list(assets)
        try {
            return stream.filter {
                Files.isRegularFile(it) && it.fileName.toString().endsWith(".atlas")
            }
                .sorted().toArray().map { it as Path }
        } finally {
            stream.close()
        }
    }

    private fun inspect(atlas: Path): AtlasInfo {
        val parent = atlas.toFile().canonicalFile.parentFile.toPath()
        val data = TextureAtlas.TextureAtlasData(
            FileHandle(atlas.toFile()),
            FileHandle(parent.toFile()),
            false
        )
        require(data.pages.size > 0 && data.regions.size > 0) { "Пустой атлас: $atlas" }
        val pages = linkedSetOf<Path>()
        for (page in data.pages) {
            val path = page.textureFile.file().canonicalFile.toPath()
            require(path.parent == parent && Files.isRegularFile(path)) { "Недопустимая страница атласа: $path" }
            pages.add(path)
            val image = requireNotNull(ImageIO.read(path.toFile())) { "Не удалось прочитать $path" }
            try {
                for (region in data.regions) {
                    if (region.page !== page) continue
                    val width = if (region.rotate) region.height else region.width
                    val height = if (region.rotate) region.width else region.height
                    require(
                        width > 0 && height > 0 && region.left >= 0 && region.top >= 0 &&
                                region.left.toLong() + width <= image.width && region.top.toLong() + height <= image.height
                    ) {
                        "Область ${region.name} выходит за пределы $path"
                    }
                }
            } finally {
                image.flush()
            }
        }
        val keys = data.regions.map { "${it.name}#${it.index}" }
        require(keys.distinct().size == keys.size) { "Повторяющиеся области в $atlas" }
        return AtlasInfo(pages, keys.toSet())
    }

    fun prepare(
        projectRoot: Path,
        edits: List<ImageEdit>,
        progress: (String) -> Unit = {}
    ): Prepared {
        if (edits.isEmpty()) return Prepared(emptyList(), emptySet()) {}
        val root = projectRoot.toFile().canonicalFile.toPath()
        val assets = root.resolve("android/assets")
        val names = edits.map {
            if (it.kind == UnitImages.Kind.ICON) "Construction" else "AbsoluteUnits"
        }.toSortedSet()
        val sourceRoots = names.associateWith { name ->
            if (name == "Construction") listOf(root.resolve("android/Images.Construction"))
            else listOf(
                root.resolve("graphics/base/$name"),
                root.resolve("graphics/overrides/$name")
            )
        }
        sourceRoots.values.forEach { directories ->
            require(Files.isDirectory(directories.first())) { "Отсутствуют исходники: ${directories.first()}" }
        }
        val originalAtlasList = atlasFiles(assets)
        val observed = linkedMapOf<Path, String?>()
        fun observe(path: Path): ByteArray? {
            val bytes = read(path)
            val fingerprint = bytes?.let { hash(it) }
            if (observed.containsKey(path)) check(observed[path] == fingerprint) { "Файл изменился: $path" }
            observed[path] = fingerprint
            return bytes
        }

        val installed = linkedMapOf<String, AtlasInfo>()
        val ownedByOthers = linkedSetOf<Path>()
        for (atlas in originalAtlasList) {
            observe(atlas)
            val name = atlas.fileName.toString().removeSuffix(".atlas")
            val info = inspect(atlas)
            if (name in names) {
                installed[name] = info
                info.pages.forEach { observe(it) }
            } else {
                ownedByOthers.add(atlas)
                ownedByOthers.addAll(info.pages)
            }
        }
        val registryPath = assets.resolve("Atlases.json")
        val registryBytes = observe(registryPath)
        val registry = if (registryBytes == null) mutableListOf<String>() else {
            val node = UnitDocument.parse(registryBytes.toString(Charsets.UTF_8))
            require(node.isArray && node.all { it.isTextual }) { "Atlases.json должен содержать список имён" }
            node.map { it.asText() }.toMutableList()
        }
        val workRoot = root.resolve("build/graphics/unit-editor")
        Files.createDirectories(workRoot)
        val work = Files.createTempDirectory(workRoot, "save-")
        val output = Files.createDirectory(work.resolve("atlases"))
        val sourceListings = linkedMapOf<Path, Map<String, Path>>()
        val changes = mutableListOf<UnitFileTransaction.Change>()
        val newTargets = linkedSetOf<Path>()
        for (name in names) {
            progress("Подготовка $name…")
            val combined = sortedMapOf<String, Path>()
            for (directory in sourceRoots.getValue(name)) {
                val entries = sources(directory)
                sourceListings[directory] = entries
                combined.putAll(entries)
            }
            val staged = Files.createDirectory(work.resolve(name))
            for ((relative, path) in combined) {
                val bytes = requireNotNull(observe(path)) { "Исходник исчез: $path" }
                val target = staged.resolve(relative)
                Files.createDirectories(target.parent)
                Files.write(target, bytes)
            }
            val relevant = edits.filter {
                (if (it.kind == UnitImages.Kind.ICON) "Construction" else "AbsoluteUnits") == name
            }
            for (edit in relevant) {
                val destination = edit.target.toFile().canonicalFile.toPath()
                val expectedRoot = root.resolve(edit.kind.directory).toFile().canonicalFile.toPath()
                require(
                    destination.parent == expectedRoot && destination.fileName.toString()
                        .endsWith(".png")
                ) {
                    "Недопустимый путь изображения: $destination"
                }
                check(
                    same(
                        observe(destination),
                        edit.expected
                    )
                ) { "Изображение изменено извне: $destination" }
                val prefix =
                        if (edit.kind == UnitImages.Kind.ICON) "UnitIcons" else "TileSets/AbsoluteUnits/Units"
                val stagedImage = staged.resolve("$prefix/${destination.fileName}")
                Files.createDirectories(stagedImage.parent)
                Files.write(stagedImage, edit.bytes)
                changes.add(UnitFileTransaction.Change(destination, edit.expected, edit.bytes))
            }
            val paths = sources(staged).keys
            require(paths.groupBy { it.lowercase(Locale.ROOT) }.values.none { it.size > 1 }) {
                "Имена исходников $name отличаются только регистром"
            }
            val imageKeys = paths.filter {
                it.substringAfterLast('.').lowercase(Locale.ROOT) in setOf("png", "jpg", "jpeg")
            }.groupBy { it.substringBeforeLast('.').removeSuffix(".9") }
            require(imageKeys.values.none { it.size > 1 }) { "Несколько изображений создают одну область в $name" }
            val defaults = TexturePacker.Settings().apply {
                maxWidth = 2048
                maxHeight = 2048
                combineSubdirectories = true
                pot = true
                fast = true
                rotation = false
                paddingX = 8
                paddingY = 8
                duplicatePadding = true
                filterMin = Texture.TextureFilter.MipMapLinearLinear
                filterMag = Texture.TextureFilter.MipMapLinearLinear
            }
            val custom = staged.resolve("TexturePacker.settings")
            val settings = if (Files.isRegularFile(custom)) Files.newBufferedReader(custom).use {
                Json().fromJson(TexturePacker.Settings::class.java, it)
            } else defaults
            progress("Упаковка $name…")
            TexturePacker.process(settings, staged.toString(), output.toString(), name)
            val generatedAtlas = output.resolve("$name.atlas")
            val generated = inspect(generatedAtlas)
            val missing = installed[name]?.regions.orEmpty() - generated.regions
            require(missing.isEmpty()) {
                "$name: в исходниках не хватает ${missing.size} прежних областей: ${
                    missing.take(10).joinToString()
                }. Файлы проекта не изменены."
            }
            for (edit in relevant) {
                val prefix =
                        if (edit.kind == UnitImages.Kind.ICON) "UnitIcons" else "TileSets/AbsoluteUnits/Units"
                val key = "$prefix/${edit.target.fileName.toString().removeSuffix(".png")}#-1"
                require(key in generated.regions) { "В новом атласе отсутствует $key" }
            }
            // Path implements Iterable<Path>; wrap it to avoid adding its individual components.
            for (file in generated.pages + setOf(generatedAtlas)) {
                val target = assets.resolve(file.fileName)
                require(target !in ownedByOthers && newTargets.add(target)) {
                    "Страница принадлежит другому атласу: $target"
                }
                changes.add(
                    UnitFileTransaction.Change(
                        target,
                        observe(target),
                        Files.readAllBytes(file)
                    )
                )
            }
            if (name !in registry) registry.add(name)
        }
        val obsolete = installed.values.flatMap { it.pages }.toSet() - newTargets - ownedByOthers
        obsolete.forEach { changes.add(UnitFileTransaction.Change(it, observe(it), null)) }
        changes.add(
            UnitFileTransaction.Change(
                registryPath, registryBytes, UnitDocument.mapper.writeValueAsBytes(registry)
            )
        )
        val ordered = changes.sortedBy {
            when {
                !it.target.startsWith(assets) -> 0
                it.target.fileName.toString().endsWith(".png") -> 1
                it.target.fileName.toString().endsWith(".atlas") -> 2
                else -> 3
            }
        }
        return Prepared(ordered, names) {
            check(atlasFiles(assets) == originalAtlasList) { "Список атласов изменился во время упаковки" }
            for ((directory, listing) in sourceListings) {
                check(sources(directory) == listing) { "Список исходников изменился: $directory" }
            }
            for ((path, fingerprint) in observed) {
                check(read(path)?.let { hash(it) } == fingerprint) { "Файл изменился во время упаковки: $path" }
            }
        }
    }
}
