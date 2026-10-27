package com.curios.mod

import com.curios.mod.logic.CuriosLoad
import com.curios.mod.logic.EntityFile
import com.curios.mod.logic.SlotFile
import com.curios.mod.logic.loadCurios
import com.curios.mod.logic.validatorAllows
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

object CuriosRuntime {
    @Volatile
    var loaded: CuriosLoad = CuriosLoad(emptyMap(), emptyMap())

    fun slotsFor(type: EntityType<*>, tags: Set<String>): List<SlotRef> {
        val id = EntityType.getKey(type).toString()
        val names = linkedSetOf<String>()
        loaded.entitySlots[id]?.let(names::addAll)
        for (tag in tags) loaded.entitySlots["#$tag"]?.let(names::addAll)
        val refs = mutableListOf<SlotRef>()
        for (name in names) {
            val slot = loaded.slots[name] ?: continue
            repeat(slot.size) { index -> refs += SlotRef(name, index, false) }
        }
        return refs
    }

    fun allows(stack: ItemStack, slotId: String): Boolean {
        if (stack.isEmpty) return false
        val slot = loaded.slots[slotId] ?: return false
        val tags = mutableSetOf<String>()
        stack.tags.forEach { tags += it.location.toString() }
        return validatorAllows(slot.validators, slotId, tags)
    }

    /**
     * A dedicated client never runs the server data reload. Read the same files from installed jars
     * so Trinkets callers can see the slots before the integrated server replaces this list.
     */
    fun loadBundledData() {
        val slots = mutableListOf<SlotFile>()
        val entities = mutableListOf<EntityFile>()
        for (mod in FabricLoader.getInstance().allMods) {
            for (root in mod.rootPaths) {
                try {
                    collectCuriosFiles(root, slots, entities)
                } catch (failure: Exception) {
                    curiosDataLog.warn("Could not read curios data from {}", mod.metadata.id, failure)
                }
            }
        }
        if (slots.isEmpty() && entities.isEmpty()) return
        loaded = loadCurios(slots, entities)
    }
}

private val curiosDataLog = LoggerFactory.getLogger("curios")

fun visibleSlotRefs(dataRefs: List<SlotRef>, stored: List<SlotRef>): List<SlotRef> {
    val seen = HashSet<SlotRef>()
    val result = ArrayList<SlotRef>(dataRefs.size + stored.size)
    for (ref in dataRefs) {
        if (seen.add(ref)) result += ref
    }
    for (ref in stored) {
        if (seen.add(ref)) result += ref
    }
    return result
}

private fun collectCuriosFiles(root: Path, slots: MutableList<SlotFile>, entities: MutableList<EntityFile>) {
    val data = root.resolve("data")
    if (!Files.isDirectory(data)) return
    Files.newDirectoryStream(data).use { namespaces ->
        for (namespace in namespaces) {
            if (!Files.isDirectory(namespace)) continue
            val name = namespace.fileName?.toString() ?: continue
            readCuriosDirectory(namespace.resolve("curios").resolve("slots"), name, slots, entities, slotsDirectory = true)
            readCuriosDirectory(namespace.resolve("curios").resolve("entities"), name, slots, entities, slotsDirectory = false)
        }
    }
}

private fun readCuriosDirectory(
    directory: Path,
    namespace: String,
    slots: MutableList<SlotFile>,
    entities: MutableList<EntityFile>,
    slotsDirectory: Boolean,
) {
    if (!Files.isDirectory(directory)) return
    Files.newDirectoryStream(directory, "*.json").use { files ->
        for (file in files) {
            if (!Files.isRegularFile(file)) continue
            val id = file.fileName.toString().removeSuffix(".json")
            if ('/' in id || '\\' in id) continue
            val text = Files.readString(file)
            if (slotsDirectory) slots += SlotFile(namespace, id, text) else entities += EntityFile(namespace, id, text)
        }
    }
}

data class SlotRef(val id: String, val index: Int, val cosmetic: Boolean) {
    fun key(): String = if (cosmetic) "$id#cosmetic" else id
}

fun slotRef(key: String, index: Int): SlotRef {
    val cosmetic = key.endsWith("#cosmetic")
    val id = if (cosmetic) key.removeSuffix("#cosmetic") else key
    return SlotRef(id, index, cosmetic)
}

class CuriosReload : IdentifiableResourceReloadListener {
    override fun getFabricId(): ResourceLocation = ResourceLocation.fromNamespaceAndPath("curios", "data")

    override fun reload(
        barrier: PreparableReloadListener.PreparationBarrier,
        manager: ResourceManager,
        preparationsProfiler: net.minecraft.util.profiling.ProfilerFiller,
        reloadProfiler: net.minecraft.util.profiling.ProfilerFiller,
        backgroundExecutor: Executor,
        gameExecutor: Executor,
    ): CompletableFuture<Void> {
        return CompletableFuture.supplyAsync({ read(manager) }, backgroundExecutor)
            .thenCompose { barrier.`wait`(it) }
            .thenAcceptAsync({ CuriosRuntime.loaded = it }, gameExecutor)
    }

    private fun read(manager: ResourceManager): CuriosLoad {
        val slots = mutableListOf<SlotFile>()
        val entities = mutableListOf<EntityFile>()
        for ((location, resource) in manager.listResources("curios") { it.path.endsWith(".json") }) {
            val text = resource.openAsReader().use { it.readText() }
            val path = location.path
            if (path.startsWith("curios/slots/")) {
                val id = path.removePrefix("curios/slots/").removeSuffix(".json")
                if ('/' !in id) slots += SlotFile(location.namespace, id, text)
            } else if (path.startsWith("curios/entities/")) {
                val id = path.removePrefix("curios/entities/").removeSuffix(".json")
                if ('/' !in id) entities += EntityFile(location.namespace, id, text)
            }
        }
        return loadCurios(slots, entities)
    }
}

fun entityTags(type: EntityType<*>): Set<String> {
    val tags = mutableSetOf<String>()
    type.builtInRegistryHolder().tags().forEach { tags += it.location.toString() }
    return tags
}
