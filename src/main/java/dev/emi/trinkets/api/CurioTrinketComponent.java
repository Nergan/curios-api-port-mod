package dev.emi.trinkets.api;

import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class CurioTrinketComponent implements TrinketComponent {
    private final LivingEntity entity;

    public CurioTrinketComponent(LivingEntity entity) {
        this.entity = entity;
    }

    @Override
    public Map<String, Map<String, TrinketInventory>> getInventory() {
        Map<String, Map<String, TrinketInventory>> groups = new LinkedHashMap<>();
        for (CurioSlotView view : TrinketsApi.slots().slots(entity)) {
            groups.computeIfAbsent(view.group(), key -> new LinkedHashMap<>())
                    .put(view.name(), new TrinketInventory(entity, view));
        }
        return groups;
    }

    @Override
    public List<Tuple<SlotReference, ItemStack>> getAllEquipped() {
        List<Tuple<SlotReference, ItemStack>> equipped = new ArrayList<>();
        for (Map<String, TrinketInventory> group : getInventory().values()) {
            for (TrinketInventory inventory : group.values()) {
                ItemStack stack = inventory.getItem(0);
                if (!stack.isEmpty()) {
                    equipped.add(new Tuple<>(new SlotReference(inventory, 0), stack));
                }
            }
        }
        return equipped;
    }

    @Override
    public List<Tuple<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate) {
        List<Tuple<SlotReference, ItemStack>> equipped = new ArrayList<>();
        for (Tuple<SlotReference, ItemStack> pair : getAllEquipped()) {
            if (predicate.test(pair.getB())) {
                equipped.add(pair);
            }
        }
        return equipped;
    }
}
