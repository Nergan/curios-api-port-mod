package com.curios.mod

import com.curios.mod.logic.trinketGroupAndName
import dev.emi.trinkets.api.CurioSlotView
import dev.emi.trinkets.api.CurioSlots
import dev.emi.trinkets.api.SlotReference
import dev.emi.trinkets.api.TrinketInventory
import dev.emi.trinkets.api.TrinketsApi
import dev.emi.trinkets.api.event.TrinketEquipCallback
import dev.emi.trinkets.api.event.TrinketUnequipCallback
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import top.theillusivec4.curios.api.type.capability.ICurio

object CuriosTrinkets {
    fun install() {
        TrinketsApi.bind(object : CurioSlots {
            override fun slots(entity: LivingEntity): List<CurioSlotView> {
                return CuriosRuntime.slotsFor(entity.type, entityTags(entity.type)).map { ref ->
                    val (group, name) = trinketGroupAndName(ref.id, ref.index)
                    CurioSlotView(ref.id, ref.index, group, name)
                }
            }

            override fun stack(entity: LivingEntity, id: String, index: Int): ItemStack {
                val inventory = entity.getAttached(CuriosMod.INVENTORY) ?: return ItemStack.EMPTY
                return inventory.stack(SlotRef(id, index, false))
            }

            override fun set(entity: LivingEntity, id: String, index: Int, stack: ItemStack) {
                change(entity, SlotRef(id, index, false), stack)
            }
        })
    }

    fun reference(entity: LivingEntity, ref: SlotRef): SlotReference {
        val (group, name) = trinketGroupAndName(ref.id, ref.index)
        return SlotReference(TrinketInventory(entity, CurioSlotView(ref.id, ref.index, group, name)), 0)
    }

    fun change(entity: LivingEntity, ref: SlotRef, stack: ItemStack) {
        val inventory = entity.getAttachedOrCreate(CuriosMod.INVENTORY)
        val previous = inventory.stack(ref)
        if (previous === stack || (previous.isEmpty && stack.isEmpty)) {
            return
        }
        inventory.set(ref, stack)
        if (!ItemStack.matches(previous, stack)) {
            val slotContext = context(entity, ref)
            (previous.item as? ICurio)?.onUnequip(slotContext, stack)
            (stack.item as? ICurio)?.onEquip(slotContext, previous)
            val slotReference = reference(entity, ref)
            if (!previous.isEmpty) {
                TrinketUnequipCallback.EVENT.invoker().onUnequip(previous, slotReference, entity)
            }
            if (!stack.isEmpty) {
                TrinketEquipCallback.EVENT.invoker().onEquip(stack, slotReference, entity)
            }
            inventory.refreshAttributes(entity, CuriosRuntime.slotsFor(entity.type, entityTags(entity.type)))
        }
        entity.setAttached(CuriosMod.INVENTORY, inventory)
    }
}
