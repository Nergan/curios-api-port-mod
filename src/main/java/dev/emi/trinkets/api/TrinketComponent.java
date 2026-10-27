package dev.emi.trinkets.api;

import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public interface TrinketComponent {
    Map<String, Map<String, TrinketInventory>> getInventory();

    List<Tuple<SlotReference, ItemStack>> getAllEquipped();

    List<Tuple<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate);
}
