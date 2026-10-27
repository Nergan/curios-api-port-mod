package com.curios.mod

import com.mojang.datafixers.util.Pair as MojangPair
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.Container
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents
import net.minecraft.world.item.enchantment.EnchantmentHelper
import top.theillusivec4.curios.api.type.capability.ICurio

class CurioContainer(
    val owner: Player,
    val layout: List<SlotRef>,
) : Container {
    private val curios: CuriosInventory
        get() = CuriosMod.inventory(owner)
    override fun getContainerSize(): Int = layout.size

    override fun isEmpty(): Boolean = layout.all { curios.stack(it).isEmpty }

    override fun getItem(slot: Int): ItemStack = curios.stack(layout[slot])

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val current = getItem(slot)
        if (current.isEmpty || amount <= 0) return ItemStack.EMPTY
        val taken = current.copyWithCount(minOf(amount, current.count))
        val remainder = current.copy()
        remainder.shrink(taken.count)
        setItem(slot, if (remainder.isEmpty) ItemStack.EMPTY else remainder)
        return taken
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val taken = getItem(slot).copy()
        if (!taken.isEmpty) setItem(slot, ItemStack.EMPTY)
        return taken
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        CuriosTrinkets.change(owner, layout[slot], stack)
        setChanged()
    }

    override fun setChanged() {
        owner.setAttached(CuriosMod.INVENTORY, curios)
    }

    override fun stillValid(player: Player): Boolean = player === owner

    override fun clearContent() {
        for (index in layout.indices) setItem(index, ItemStack.EMPTY)
    }
}

class CuriosMenu(
    syncId: Int,
    playerInventory: Inventory,
    val layout: List<SlotRef>,
) : AbstractContainerMenu(CuriosMod.MENU, syncId) {
    val curioCount: Int

    init {
        val curios = CurioContainer(playerInventory.player, layout)
        for (index in layout.indices) {
            val (x, y) = curioInventoryPosition(index, layout.size)
            addSlot(CurioSlot(curios, index, x, y))
        }
        curioCount = layout.size
        val player = playerInventory.player
        val armor = listOf(
            EquipmentSlot.HEAD to InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            EquipmentSlot.CHEST to InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            EquipmentSlot.LEGS to InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            EquipmentSlot.FEET to InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
        )
        for ((index, entry) in armor.withIndex()) {
            val (equipment, icon) = entry
            addSlot(ArmorEquipmentSlot(playerInventory, player, equipment, 39 - index, 8, 8 + index * 18, icon))
        }
        addSlot(OffhandEquipmentSlot(playerInventory, player, 40, 77, 62))
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    fun panelHeight(): Int = CURIO_TOP + CURIO_VISIBLE_ROWS * 18 + 14 + 76 + 8

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY
        val equipmentEnd = curioCount + VANILLA_EQUIPMENT_COUNT
        val inventoryStart = equipmentEnd
        if (index < curioCount) {
            val moving = slot.item.copy()
            val taken = moving.copy()
            slot.set(ItemStack.EMPTY)
            if (!moveItemStackTo(moving, inventoryStart, slots.size, true)) {
                slot.set(moving)
                return ItemStack.EMPTY
            }
            if (!moving.isEmpty) slot.set(moving)
            return taken
        }
        val original = slot.item.copy()
        val stack = slot.item
        val moved = if (index < equipmentEnd) {
            moveItemStackTo(stack, inventoryStart, slots.size, true)
        } else {
            moveItemStackTo(stack, curioCount, equipmentEnd, false) ||
                moveItemStackTo(stack, 0, curioCount, false)
        }
        if (!moved) return ItemStack.EMPTY
        if (stack.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
        return original
    }

    override fun stillValid(player: Player): Boolean = true
}

class CurioSlot(container: CurioContainer, index: Int, x: Int, y: Int) : Slot(container, index, x, y) {
    private val ref: SlotRef = container.layout[index]
    private val owner: Player = container.owner

    override fun mayPlace(stack: ItemStack): Boolean {
        val curio = stack.item as? ICurio
        if (curio != null && !curio.canEquip(context(owner, ref))) return false
        return CuriosRuntime.allows(stack, ref.id)
    }

    override fun mayPickup(player: Player): Boolean {
        val curio = item.item as? ICurio ?: return true
        return curio.canUnequip(context(owner, ref))
    }

    override fun getMaxStackSize(stack: ItemStack): Int = 1

    override fun isActive(): Boolean = x > -1000

    override fun getNoItemIcon(): MojangPair<ResourceLocation, ResourceLocation>? {
        val icon = if (ref.cosmetic) {
            "curios:slot/empty_cosmetic_slot"
        } else {
            CuriosRuntime.loaded.slots[ref.id]?.icon ?: "curios:slot/empty_curio_slot"
        }
        val sprite = ResourceLocation.tryParse(icon) ?: return null
        return MojangPair.of(InventoryMenu.BLOCK_ATLAS, sprite)
    }
}

const val CURIO_VISIBLE_ROWS = 7
const val CURIO_TOP = 18
const val VANILLA_EQUIPMENT_COUNT = 5

private class ArmorEquipmentSlot(
    inventory: Container,
    private val owner: Player,
    private val equipment: EquipmentSlot,
    index: Int,
    x: Int,
    y: Int,
    private val emptyIcon: ResourceLocation,
) : Slot(inventory, index, x, y) {
    override fun getMaxStackSize(): Int = 1

    override fun mayPlace(stack: ItemStack): Boolean = owner.getEquipmentSlotForItem(stack) == equipment

    override fun mayPickup(player: Player): Boolean {
        val worn = item
        val locked = !worn.isEmpty && EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
        return (!locked || player.isCreative) && super.mayPickup(player)
    }

    override fun setByPlayer(stack: ItemStack, previous: ItemStack) {
        owner.onEquipItem(equipment, stack, previous)
        super.setByPlayer(stack, previous)
    }

    override fun getNoItemIcon(): MojangPair<ResourceLocation, ResourceLocation> =
        MojangPair.of(InventoryMenu.BLOCK_ATLAS, emptyIcon)
}

private class OffhandEquipmentSlot(
    inventory: Container,
    private val owner: Player,
    index: Int,
    x: Int,
    y: Int,
) : Slot(inventory, index, x, y) {
    override fun setByPlayer(stack: ItemStack, previous: ItemStack) {
        owner.onEquipItem(EquipmentSlot.OFFHAND, stack, previous)
        super.setByPlayer(stack, previous)
    }

    override fun getNoItemIcon(): MojangPair<ResourceLocation, ResourceLocation> =
        MojangPair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD)
}

fun curioSlotPosition(index: Int, scroll: Int): Pair<Int, Int> {
    val row = index - scroll
    return if (row in 0 until CURIO_VISIBLE_ROWS) 8 to (CURIO_TOP + row * 18) else -2000 to -2000
}

object CuriosLayout {
    val CODEC: StreamCodec<RegistryFriendlyByteBuf, List<SlotRef>> = StreamCodec.of(
        { buf, refs ->
            buf.writeVarInt(refs.size)
            for (ref in refs) {
                buf.writeUtf(ref.id)
                buf.writeVarInt(ref.index)
                buf.writeBoolean(ref.cosmetic)
            }
        },
        { buf ->
            val count = buf.readVarInt()
            List(count) { SlotRef(buf.readUtf(), buf.readVarInt(), buf.readBoolean()) }
        },
    )
}
