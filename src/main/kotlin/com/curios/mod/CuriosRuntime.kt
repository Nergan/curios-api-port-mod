package com.curios.mod

import com.curios.mod.logic.CuriosLoad
import com.curios.mod.logic.EntityFile
import com.curios.mod.logic.SlotFile
import com.curios.mod.logic.loadCurios
import com.curios.mod.logic.validatorAllows
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
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
}

data class SlotRef(val id: String, val index: Int, val cosmetic: Boolean) {
    fun key(): String = if (cosmetic) "$id#cosmetic" else id
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
