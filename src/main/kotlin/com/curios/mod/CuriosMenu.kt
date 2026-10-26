package com.curios.mod

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import top.theillusivec4.curios.api.type.capability.ICurio

class CurioContainer(
    val owner: Player,
    private val curios: CuriosInventory,
    val layout: List<SlotRef>,
) : Container {
    override fun getContainerSize(): Int = layout.size

    override fun isEmpty(): Boolean = layout.all { curios.stack(it).isEmpty }

    override fun getItem(slot: Int): ItemStack = curios.stack(layout[slot])

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val stack = getItem(slot)
        if (stack.isEmpty) return ItemStack.EMPTY
        val split = stack.split(amount)
        setItem(slot, stack)
        return split
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val stack = getItem(slot)
        setItem(slot, ItemStack.EMPTY)
        return stack
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        val ref = layout[slot]
        val previous = curios.stack(ref)
        curios.set(ref, stack)
        if (!ItemStack.matches(previous, stack)) {
            val slotContext = context(owner, ref)
            (previous.item as? ICurio)?.onUnequip(slotContext, stack)
            (stack.item as? ICurio)?.onEquip(slotContext, previous)
            curios.refreshAttributes(owner, CuriosRuntime.slotsFor(owner.type, entityTags(owner.type)))
        }
        setChanged()
    }

    override fun setChanged() {}

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
        val curios = CurioContainer(playerInventory.player, CuriosMod.inventory(playerInventory.player), layout)
        var x = 8
        var y = 18
        var column = 0
        for (index in layout.indices) {
            addSlot(CurioSlot(curios, index, x, y))
            column++
            x += 18
            if (column == 9) {
                column = 0
                x = 8
                y += 18
            }
        }
        curioCount = layout.size
        val playerY = y + 14
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerY + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, playerY + 58))
        }
    }

    fun panelHeight(): Int {
        val rows = if (layout.isEmpty()) 1 else (layout.size + 8) / 9
        return 18 + rows * 18 + 14 + 76 + 8
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots[index]
        if (!slot.hasItem()) return ItemStack.EMPTY
        val original = slot.item.copy()
        val stack = slot.item
        val moved = if (index < curioCount) {
            moveItemStackTo(stack, curioCount, slots.size, true)
        } else {
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
