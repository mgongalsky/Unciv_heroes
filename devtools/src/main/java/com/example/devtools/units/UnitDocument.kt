package com.example.devtools.units

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Edits the JSON tree rather than a DTO so fields unknown to the editor survive. */
class UnitDocument(val file: Path) {
    companion object {
        val mapper = ObjectMapper()
        val numericFields = linkedSetOf(
            "cost", "hurryCostModifier", "movement", "strength", "attackSkill",
            "defenceSkill", "health", "speed", "damage", "rangedStrength",
            "religiousStrength", "range", "interceptRange"
        )
        val stringFields = linkedSetOf(
            "name", "unitType", "requiredTech", "requiredResource", "obsoleteTech",
            "upgradesTo", "replaces", "uniqueTo", "attackSound", "replacementTextForUniques"
        )
        val listFields = linkedSetOf("uniques", "promotions")

        fun parse(text: String): JsonNode = mapper.readTree(
            JsonReader().parse(text).toJson(JsonWriter.OutputType.json)
        )

        fun validateName(name: String) {
            require(name.isNotBlank() && name == name.trim()) { "Имя не должно быть пустым или иметь пробелы по краям" }
            require(name.none { it.code < 32 || it in "<>:\"/\\|?*" } && !name.endsWith('.')) {
                "Имя содержит символы, недопустимые в имени изображения"
            }
            val stem = name.substringBefore('.').uppercase(Locale.ROOT)
            require(
                stem !in setOf("CON", "PRN", "AUX", "NUL") &&
                        !stem.matches(Regex("(COM|LPT)[0-9]"))
            ) { "Зарезервированное имя файла: $name" }
        }

        /** Replacement is atomic. If the filesystem cannot do this, leave the original intact. */
        fun atomicWrite(target: Path, bytes: ByteArray) {
            val absolute = target.toAbsolutePath()
            Files.createDirectories(absolute.parent)
            val temporary = Files.createTempFile(absolute.parent, ".unit-editor-", ".tmp")
            try {
                Files.write(temporary, bytes)
                Files.move(
                    temporary,
                    absolute,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
    }

    private var originalBytes = Files.readAllBytes(file)
    val units: MutableList<ObjectNode>
    private var savedTree: ArrayNode
    val references: Map<String, List<String>>

    init {
        val root = parse(originalBytes.toString(Charsets.UTF_8))
        require(root.isArray && root.all { it.isObject }) { "Units.json должен содержать массив объектов" }
        units = root.map { it.deepCopy<ObjectNode>() }.toMutableList()
        savedTree = tree()
        references = loadReferences()
    }

    fun tree(): ArrayNode = mapper.createArrayNode().also { result ->
        units.forEach { result.add(it.deepCopy()) }
    }

    fun isDirty() = tree() != savedTree

    fun choices(field: String): List<String> = when (field) {
        "upgradesTo", "replaces" -> units.map { it.path("name").asText() }.sorted()
        else -> references[field].orEmpty()
    }

    private fun loadReferences(): Map<String, List<String>> {
        fun names(filename: String, nested: String? = null): List<String> {
            val source = file.toAbsolutePath().parent.resolve(filename)
            if (!Files.isRegularFile(source)) return emptyList()
            val root = parse(Files.readString(source))
            require(root.isArray) { "$filename должен содержать массив" }
            val entries =
                if (nested == null) root.toList() else root.flatMap { it.path(nested).toList() }
            return entries.mapNotNull {
                it.get("name")?.takeIf { value -> value.isTextual }?.asText()
            }
                .distinct().sorted()
        }

        val technologies = names("Techs.json", "techs")
        return mapOf(
            "unitType" to names("UnitTypes.json"),
            "requiredTech" to technologies, "obsoleteTech" to technologies,
            "requiredResource" to names("TileResources.json"),
            "uniqueTo" to names("Nations.json"), "promotions" to names("UnitPromotions.json")
        )
    }

    /** Only call for changed controls: untouched values retain their exact JSON type. */
    fun setText(unit: ObjectNode, field: String, text: String) {
        if (field in numericFields) {
            if (text.isBlank()) {
                unit.remove(field); return
            }
            val number =
                text.trim().toIntOrNull() ?: error("$field: требуется целое число в диапазоне Int")
            require(field == "hurryCostModifier" || number >= 0) { "$field: значение не может быть отрицательным" }
            if (unit.get(field)?.isTextual == true) unit.put(field, number.toString())
            else unit.put(field, number)
        } else {
            unit.put(field, text)
        }
    }

    fun validate(): List<String> =
            validationErrors().map { "Ошибка: $it" } +
                    validationWarnings().map { "Предупреждение (не мешает сохранению): $it" }

    fun addUnit(name: String, template: ObjectNode? = null): ObjectNode {
        validateName(name)
        require(units.none { it.path("name").asText().equals(name, true) }) {
            "Юнит с таким именем уже существует"
        }
        val unit: ObjectNode
        if (template != null) {
            unit = template.deepCopy()
        } else {
            unit = mapper.createObjectNode()
            unit.put("unitType", choices("unitType").firstOrNull().orEmpty())
            unit.put("movement", 2)
            unit.put("speed", 5)
            unit.put("health", 20)
            unit.put("damage", 5)
            unit.put("cost", 40)
        }
        unit.put("name", name)
        units.add(unit)
        return unit
    }

    fun backup(directory: Path): Path {
        Files.createDirectories(directory)
        val id = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")) + "-" + UUID.randomUUID()
        val snapshot = Files.createDirectory(directory.resolve(id))
        val paths = Files.list(file.toAbsolutePath().parent)
        try {
            paths.filter {
                Files.isRegularFile(it) && it.fileName.toString().endsWith(".json", true)
            }
                .forEach { source ->
                    val bytes = Files.readAllBytes(source)
                    val target = snapshot.resolve(source.fileName)
                    Files.write(target, bytes)
                    check(
                        Files.readAllBytes(target).contentEquals(bytes)
                    ) { "Ошибка проверки копии $target" }
                }
        } finally {
            paths.close()
        }
        Files.writeString(snapshot.resolve("source.txt"), file.toAbsolutePath().parent.toString())
        return snapshot
    }

    fun save(backupDirectory: Path) {
        val errors = validationErrors()
        require(errors.isEmpty()) { errors.joinToString("\n") }
        check(Files.readAllBytes(file).contentEquals(originalBytes)) {
            "Units.json изменён другой программой. Откройте файл заново; текущие правки можно экспортировать отдельно."
        }
        val bytes = serialized()
        backup(backupDirectory)
        check(
            Files.readAllBytes(file).contentEquals(originalBytes)
        ) { "Units.json изменился во время резервного копирования" }
        atomicWrite(file, bytes)
        originalBytes = bytes
        savedTree = tree()
    }

    fun serialized(): ByteArray =
        (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree()) + "\n")
            .toByteArray(Charsets.UTF_8)
    fun validationErrors(): List<String> {
        val errors = mutableListOf<String>()
        val names = mutableSetOf<String>()
        for (unit in units) {
            val name = unit.path("name").asText()
            runCatching { validateName(name) }.exceptionOrNull()
                ?.let { errors += "$name: ${it.message}" }
            if (!names.add(name.lowercase(Locale.ROOT))) errors += "Повторяющееся имя: $name"
            for (field in numericFields) {
                val value = unit.get(field) ?: continue
                val number = value.asText().toIntOrNull()
                if (number == null || (field != "hurryCostModifier" && number < 0))
                    errors += "$name / $field: требуется допустимое целое число"
            }
            for (field in stringFields) {
                val value = unit.get(field) ?: continue
                if (!value.isTextual && !value.isNull) errors += "$name / $field: требуется строка"
            }
            val type = unit.get("unitType")
            if (type == null || !type.isTextual || type.asText()
                        .isBlank()
            ) errors += "$name: не выбран unitType"
            for (field in listFields) {
                val value = unit.get(field) ?: continue
                if (!value.isArray || value.any { !it.isTextual }) errors += "$name / $field: требуется список строк"
            }
            val description = unit.get("civilopediaText")
            if (description != null && (!description.isArray || description.any { !it.isObject }))
                errors += "$name / civilopediaText: требуется список объектов"
        }
        return errors
    }

    /** References are advisory: a local dictionary may be incomplete or intentionally customized. */
    fun validationWarnings(): List<String> {
        val warnings = mutableListOf<String>()
        for (unit in units) {
            val name = unit.path("name").asText()
            for (field in references.keys + listOf("upgradesTo", "replaces")) {
                if (field == "promotions") continue
                val value = unit.get(field)?.takeIf { it.isTextual }?.asText().orEmpty()
                if (value.isBlank()) continue
                val available = choices(field)
                if (available.isNotEmpty() && value !in available)
                    warnings += "$name / $field: значение $value отсутствует в справочнике редактора"
            }
            val promotions = choices("promotions")
            if (promotions.isNotEmpty()) unit.path("promotions").filter { it.isTextual }.forEach {
                if (it.asText() !in promotions)
                    warnings += "$name: повышение ${it.asText()} отсутствует в справочнике редактора"
            }
        }
        return warnings
    }
    fun prepareSave(): UnitFileTransaction.Change {
        val errors = validationErrors()
        require(errors.isEmpty()) { errors.joinToString("\n") }
        return UnitFileTransaction.Change(file, originalBytes.clone(), serialized())
    }

    /** Called on the UI thread only after the complete save transaction succeeds. */
    fun acceptSaved(change: UnitFileTransaction.Change) {
        val bytes = requireNotNull(change.bytes)
        originalBytes = bytes.clone()
        savedTree = mapper.readTree(bytes) as ArrayNode
    }
}
