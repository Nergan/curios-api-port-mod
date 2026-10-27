package dev.emi.trinkets.api.event;

import dev.emi.trinkets.api.SlotReference;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketUnequipCallback {
    Event<TrinketUnequipCallback> EVENT = EventFactory.createArrayBacked(TrinketUnequipCallback.class, listeners -> (stack, ref, entity) -> {
        for (TrinketUnequipCallback listener : listeners) {
            listener.onUnequip(stack, ref, entity);
        }
    });

    void onUnequip(ItemStack stack, SlotReference ref, LivingEntity entity);
}
