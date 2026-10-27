package dev.emi.trinkets.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface CurioSlots {
    CurioSlots EMPTY = new CurioSlots() {
        @Override
        public List<CurioSlotView> slots(LivingEntity entity) {
            return List.of();
        }

        @Override
        public ItemStack stack(LivingEntity entity, String id, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public void set(LivingEntity entity, String id, int index, ItemStack stack) {
        }
    };

    List<CurioSlotView> slots(LivingEntity entity);

    ItemStack stack(LivingEntity entity, String id, int index);

    void set(LivingEntity entity, String id, int index, ItemStack stack);
}
