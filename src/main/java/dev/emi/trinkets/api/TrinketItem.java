package dev.emi.trinkets.api;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Map;
import java.util.Optional;

public final class TrinketItem {
    private TrinketItem() {
    }

    public static boolean equipItem(LivingEntity user, ItemStack stack) {
        Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(user);
        if (optional.isEmpty()) {
            return false;
        }
        for (Map<String, TrinketInventory> group : optional.get().getInventory().values()) {
            for (TrinketInventory inventory : group.values()) {
                if (!inventory.getItem(0).isEmpty()) {
                    continue;
                }
                SlotReference ref = new SlotReference(inventory, 0);
                if (!canInsert(stack, ref, user)) {
                    continue;
                }
                ItemStack equipped = stack.copy();
                inventory.setItem(0, equipped);
                Holder<SoundEvent> sound = TrinketsApi.getTrinket(stack.getItem()).getEquipSound(stack, ref, user);
                if (sound != null) {
                    user.gameEvent(GameEvent.EQUIP);
                    user.playSound(sound.value(), 1.0F, 1.0F);
                }
                stack.setCount(0);
                return true;
            }
        }
        return false;
    }

    private static boolean canInsert(ItemStack stack, SlotReference ref, LivingEntity entity) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!TrinketsApi.getTrinket(stack.getItem()).canEquip(stack, ref, entity)) {
            return false;
        }
        return TrinketsApi.evaluatePredicateSet(ref.inventory().getSlotType().getValidatorPredicates(), stack, ref, entity);
    }
}
