package dev.emi.trinkets.api;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface Trinket {
    default void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
    }

    default void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
    }

    default void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
    }

    default boolean canEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return true;
    }

    default boolean canUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return true;
    }

    default boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
        return false;
    }

    default TrinketEnums.DropRule getDropRule(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return TrinketEnums.DropRule.DEFAULT;
    }

    default Holder<SoundEvent> getEquipSound(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return null;
    }
}
