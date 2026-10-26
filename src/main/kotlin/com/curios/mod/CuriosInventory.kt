package com.curios.mod

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.ItemStack
import top.theillusivec4.curios.api.ICuriosItemHandler
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.type.capability.ICurio
import java.util.Optional

class CuriosInventory {
    private val stacks = linkedMapOf<String, MutableList<ItemStack>>()
    private val applied = mutableListOf<Pair<Holder<Attribute>, ResourceLocation>>()

    fun stack(ref: SlotRef): ItemStack = stacks[ref.key()]?.getOrNull(ref.index) ?: ItemStack.EMPTY

    fun set(ref: SlotRef, stack: ItemStack) {
        val list = stacks.getOrPut(ref.key()) { mutableListOf() }
        while (list.size <= ref.index) list += ItemStack.EMPTY
        list[ref.index] = stack
    }

    fun replace(key: String, items: List<ItemStack>) {
        stacks[key] = items.map { it.copy() }.toMutableList()
    }

    fun groups(): List<SavedGroup> = stacks.map { (key, items) -> SavedGroup(key, items.map { it.copy() }) }

    fun ensure(refs: List<SlotRef>): List<ItemStack> {
        val needed = refs.groupBy { it.key() }.mapValues { entry -> entry.value.maxOf { it.index } + 1 }
        val dropped = mutableListOf<ItemStack>()
        for ((key, size) in needed) {
            val list = stacks.getOrPut(key) { mutableListOf() }
            while (list.size < size) list += ItemStack.EMPTY
            while (list.size > size) {
                val extra = list.removeAt(list.lastIndex)
                if (!extra.isEmpty) dropped += extra
            }
        }
        for (key in stacks.keys.filter { it !in needed }) {
            for (stack in stacks.remove(key).orEmpty()) {
                if (!stack.isEmpty) dropped += stack
            }
        }
        return dropped
    }

    fun all(): List<ItemStack> = stacks.values.flatten().filter { !it.isEmpty }

    fun refreshAttributes(entity: LivingEntity, refs: List<SlotRef>) {
        for ((holder, id) in applied) {
            entity.attributes.getInstance(holder)?.removeModifier(id)
        }
        applied.clear()
        for (ref in refs) {
            val stack = stack(ref)
            val curio = stack.item as? ICurio ?: continue
            val base = ResourceLocation.fromNamespaceAndPath("curios", "${ref.key().replace('#', '_')}_${ref.index}")
            val modifiers = curio.getAttributeModifiers(context(entity, ref), base)
            for ((holder, modifier) in modifiers.entries()) {
                val id = ResourceLocation.fromNamespaceAndPath("curios", "${base.path}_${modifier.id.path}")
                val named = AttributeModifier(id, modifier.amount, modifier.operation)
                val instance = entity.attributes.getInstance(holder) ?: continue
                instance.addOrUpdateTransientModifier(named)
                applied += holder to id
            }
        }
    }

    companion object {
        private val GROUP: Codec<SavedGroup> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("key").forGetter(SavedGroup::key),
                ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(SavedGroup::items),
            ).apply(instance, ::SavedGroup)
        }

        val CODEC: Codec<CuriosInventory> = GROUP.listOf().xmap(
            { groups ->
                CuriosInventory().also { inventory ->
                    for (group in groups) inventory.replace(group.key, group.items)
                }
            },
            { it.groups() },
        )
    }
}

data class SavedGroup(val key: String, val items: List<ItemStack>)

fun context(entity: LivingEntity, ref: SlotRef): SlotContext =
    SlotContext(ref.id, entity, ref.index, ref.cosmetic, !ref.cosmetic)

fun handler(inventory: CuriosInventory): ICuriosItemHandler = object : ICuriosItemHandler {
    override fun equipped(): List<ItemStack> = inventory.all()
    override fun isEquipped(predicate: (ItemStack) -> Boolean): Boolean = inventory.all().any(predicate)
}

fun optionalHandler(inventory: CuriosInventory?): Optional<ICuriosItemHandler> =
    if (inventory == null) Optional.empty() else Optional.of(handler(inventory))
