package com.curios.mod.logic

import com.google.gson.JsonParser

/**
 * Datapack rules from Curios 9.5.1, without NeoForge.
 * Slot files in the curios namespace are applied first. Other namespaces then modify those slots.
 * A missing size stays 1. A missing validator list means curios:tag.
 */
data class SlotFile(val namespace: String, val id: String, val json: String)

data class EntityFile(val namespace: String, val path: String, val json: String)

data class SlotTypeDef(
    val id: String,
    val size: Int,
    val order: Int,
    val icon: String,
    val addCosmetic: Boolean,
    val renderToggle: Boolean,
    val useNativeGui: Boolean,
    val dropRule: String,
    val validators: List<String>,
)

data class CuriosLoad(
    val slots: Map<String, SlotTypeDef>,
    val entitySlots: Map<String, List<String>>,
)

private class SlotBuilder(val id: String) {
    var order: Int? = null
    var size: Int? = null
    var sizeMod: Int = 0
    var icon: String? = null
    var addCosmetic: Boolean? = null
    var renderToggle: Boolean? = null
    var useNativeGui: Boolean? = null
    var dropRule: String? = null
    val validators = mutableListOf<String>()
    var validatorsSet = false

    fun order(value: Int, replace: Boolean) {
        order = if (replace || order == null) value else minOf(order!!, value)
    }

    fun size(value: Int, operation: String, replace: Boolean) {
        when (operation) {
            "SET" -> {
                size = if (replace || size == null) value else maxOf(size!!, value)
                if (replace) sizeMod = 0
            }
            "ADD" -> sizeMod = if (replace) value else sizeMod + value
            "REMOVE" -> sizeMod = if (replace) -value else sizeMod - value
            else -> throw IllegalArgumentException("$operation is not a valid operation")
        }
    }

    fun build(): SlotTypeDef {
        var builtSize = (size ?: 1) + sizeMod
        if (builtSize < 0) builtSize = 0
        val rules = if (validatorsSet) validators.toList() else listOf("curios:tag")
        return SlotTypeDef(
            id = id,
            size = builtSize,
            order = order ?: 1000,
            icon = icon ?: "curios:slot/empty_curio_slot",
            addCosmetic = addCosmetic ?: false,
            renderToggle = renderToggle ?: true,
            useNativeGui = useNativeGui ?: true,
            dropRule = dropRule ?: "DEFAULT",
            validators = rules,
        )
    }
}

fun loadCurios(slotFiles: List<SlotFile>, entityFiles: List<EntityFile>): CuriosLoad {
    val builders = linkedMapOf<String, SlotBuilder>()
    fun apply(file: SlotFile) {
        if (file.id.startsWith("_")) return
        val element = runCatching { JsonParser.parseString(file.json) }.getOrNull() ?: return
        if (!element.isJsonObject) return
        val json = element.asJsonObject
        if (json.has("neoforge:conditions")) return
        val builder = builders.getOrPut(file.id) { SlotBuilder(file.id) }
        val replace = json.get("replace")?.asBoolean ?: false
        val operation = json.get("operation")?.asString ?: "SET"
        if (json.has("order")) builder.order(json.get("order").asInt, replace)
        if (json.has("size")) {
            val size = json.get("size").asInt
            if (size < 0) return
            builder.size(size, operation, replace)
        }
        if (json.has("icon") && json.get("icon").asString.isNotEmpty()) builder.icon = json.get("icon").asString
        if (json.has("add_cosmetic")) {
            val value = json.get("add_cosmetic").asBoolean
            builder.addCosmetic = if (replace || builder.addCosmetic == null) value else builder.addCosmetic!! || value
        }
        if (json.has("render_toggle")) {
            val value = json.get("render_toggle").asBoolean
            builder.renderToggle = if (replace || builder.renderToggle == null) value else builder.renderToggle!! && value
        }
        if (json.has("use_native_gui")) {
            val value = json.get("use_native_gui").asBoolean
            builder.useNativeGui = if (replace || builder.useNativeGui == null) value else builder.useNativeGui!! && value
        }
        if (json.has("drop_rule") && json.get("drop_rule").asString.isNotEmpty()) {
            builder.dropRule = json.get("drop_rule").asString
        }
        if (json.has("validators")) {
            builder.validatorsSet = true
            for (entry in json.getAsJsonArray("validators")) builder.validators.add(entry.asString)
        }
    }
    slotFiles.filter { it.namespace == "curios" }.sortedBy { it.id }.forEach(::apply)
    slotFiles.filter { it.namespace != "curios" }.sortedWith(compareBy({ it.namespace }, { it.id })).forEach(::apply)
    val slots = builders.mapValues { it.value.build() }

    val granted = linkedMapOf<String, MutableMap<String, Boolean>>()
    for (file in entityFiles) {
        if (file.path.startsWith("_")) continue
        val element = runCatching { JsonParser.parseString(file.json) }.getOrNull() ?: continue
        if (!element.isJsonObject) continue
        val json = element.asJsonObject
        if (json.has("neoforge:conditions")) continue
        val replace = json.get("replace")?.asBoolean ?: false
        val entities = json.getAsJsonArray("entities") ?: continue
        val slotNames = json.getAsJsonArray("slots") ?: continue
        val accepted = slotNames.map { it.asString }.filter { it in slots }
        for (entity in entities) {
            val key = normalizeEntity(entity.asString)
            val current = if (replace) linkedMapOf() else granted.getOrPut(key) { linkedMapOf() }
            if (replace) granted[key] = current
            for (slot in accepted) current[slot] = true
        }
    }
    val entitySlots = granted.mapValues { (_, slotsForEntity) ->
        slotsForEntity.keys.sortedBy { slots.getValue(it).order }
    }
    return CuriosLoad(slots, entitySlots)
}

fun normalizeEntity(raw: String): String {
    val body = if (raw.startsWith("#")) raw.substring(1) else raw
    val id = if (':' in body) body else "minecraft:$body"
    return if (raw.startsWith("#")) "#$id" else id
}

fun validatorAllows(validators: List<String>, slotId: String, itemTags: Set<String>): Boolean {
    if (validators.isEmpty()) return false
    return validators.all { validator ->
        if (validator == "curios:tag") {
            "curios:$slotId" in itemTags || "curios:curio" in itemTags
        } else {
            false
        }
    }
}
