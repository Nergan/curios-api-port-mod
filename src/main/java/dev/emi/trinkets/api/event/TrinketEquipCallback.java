package dev.emi.trinkets.api.event;

import dev.emi.trinkets.api.SlotReference;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketEquipCallback {
    Event<TrinketEquipCallback> EVENT = EventFactory.createArrayBacked(TrinketEquipCallback.class, listeners -> (stack, ref, entity) -> {
        for (TrinketEquipCallback listener : listeners) {
            listener.onEquip(stack, ref, entity);
        }
    });

    void onEquip(ItemStack stack, SlotReference ref, LivingEntity entity);
}
