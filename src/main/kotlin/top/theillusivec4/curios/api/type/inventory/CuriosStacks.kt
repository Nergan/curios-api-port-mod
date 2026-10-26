package top.theillusivec4.curios.api.type.inventory

import net.minecraft.world.item.ItemStack

interface ICurioStacksHandler {
    fun getStacks(): IDynamicStackHandler
}

interface IDynamicStackHandler {
    fun getSlots(): Int

    fun getStackInSlot(slot: Int): ItemStack
}
