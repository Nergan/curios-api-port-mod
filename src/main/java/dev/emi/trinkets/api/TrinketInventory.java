package dev.emi.trinkets.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public final class TrinketInventory implements Container {
    private final LivingEntity entity;
    private final CurioSlotView view;
    private final SlotType slotType;

    public TrinketInventory(LivingEntity entity, CurioSlotView view) {
        this.entity = entity;
        this.view = view;
        this.slotType = new SlotType(view.group(), view.name(),
                Set.of(ResourceLocation.fromNamespaceAndPath("trinkets", "tag")));
    }

    public SlotType getSlotType() {
        return slotType;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getItem(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        return TrinketsApi.slots().stack(entity, view.id(), view.index());
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack split = stack.split(amount);
        setItem(slot, stack);
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }
        TrinketsApi.slots().set(entity, view.id(), view.index(), stack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        setItem(0, ItemStack.EMPTY);
    }
}
