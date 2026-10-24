package com.unciv.battle.server

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.battle.protocol.*

/** Only explicitly selected fields are decoded; client-supplied JVM types are never instantiated. */
internal object BattleHttpJson {
    fun encode(value: Any): String = Json().apply {
        setTypeName(null)
        setOutputType(JsonWriter.OutputType.json)
    }.toJson(value)

    fun objectValue(text: String): JsonValue {
        val value = try { JsonReader().parse(text) } catch (ex: RuntimeException) {
            throw IllegalArgumentException("Invalid JSON", ex)
        }
        require(value.isObject)
        return value
    }

    fun fields(value: JsonValue, allowed: Set<String>) {
        val names = value.map { it.name }
        require(names.distinct().size == names.size && names.all { it in allowed })
    }

    fun string(value: JsonValue, name: String): String {
        val field = requireNotNull(value.get(name))
        require(field.isString)
        return field.asString()
    }

    private fun number(value: JsonValue, name: String): Long {
        val field = requireNotNull(value.get(name))
        require(field.isLong) { "Expected integer $name" }
        return field.asLong()
    }

    fun integer(value: JsonValue, name: String): Int {
        val number = number(value, name)
        require(number in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
        return number.toInt()
    }

    fun command(text: String): WireBattleCommand {
        val root = objectValue(text)
        fields(root, setOf("protocolVersion", "sequence", "expectedRevision", "action", "troopId", "target", "attackFrom"))
        fun point(name: String): WirePoint? {
            val value = root.get(name)?.takeUnless { it.isNull } ?: return null
            require(value.isObject)
            fields(value, setOf("x", "y"))
            return WirePoint(integer(value, "x"), integer(value, "y"))
        }
        return WireBattleCommand(
            integer(root, "protocolVersion"), number(root, "sequence"), number(root, "expectedRevision"),
            WireAction.valueOf(string(root, "action")), integer(root, "troopId"), point("target"), point("attackFrom")
        ).also { it.validate() }
    }
    fun readyArmy(root: JsonValue): ReadyBattleArmyRequest {
        fields(root, setOf("protocolVersion", "battleNumber", "stacks"))
        val entries = requireNotNull(root.get("stacks")) { "Army stacks are required" }
        require(entries.isArray && entries.size in 1..4) { "Expected one to four stacks" }
        val stacks = entries.map { node ->
            require(node.isObject)
            fields(node, setOf("unitName", "count"))
            WireArmyStack(string(node, "unitName"), integer(node, "count"))
        }
        return ReadyBattleArmyRequest(
            integer(root, "protocolVersion"),
            integer(root, "battleNumber"),
            stacks
        )
            .also { it.validate() }
    }
}
