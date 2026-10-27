package dev.emi.trinkets.api;

import com.mojang.datafixers.util.Function3;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TrinketsApi {
    private static final Map<Item, Trinket> TRINKETS = new HashMap<>();
    private static final Map<ResourceLocation, Function3<ItemStack, SlotReference, LivingEntity, TriState>> PREDICATES = new HashMap<>();
    private static final Trinket DEFAULT_TRINKET = new Trinket() {
    };
    private static CurioSlots slots = CurioSlots.EMPTY;

    private TrinketsApi() {
    }

    public static void bind(CurioSlots slots) {
        TrinketsApi.slots = slots;
    }

    public static CurioSlots slots() {
        return slots;
    }

    public static void registerTrinket(Item item, Trinket trinket) {
        TRINKETS.put(item, trinket);
    }

    public static Trinket getTrinket(Item item) {
        return TRINKETS.getOrDefault(item, DEFAULT_TRINKET);
    }

    public static Trinket getDefaultTrinket() {
        return DEFAULT_TRINKET;
    }

    public static Optional<TrinketComponent> getTrinketComponent(LivingEntity entity) {
        if (slots.slots(entity).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CurioTrinketComponent(entity));
    }

    public static boolean evaluatePredicateSet(Set<ResourceLocation> predicates, ItemStack stack, SlotReference ref, LivingEntity entity) {
        TriState state = TriState.DEFAULT;
        for (ResourceLocation id : predicates) {
            Function3<ItemStack, SlotReference, LivingEntity, TriState> predicate = PREDICATES.get(id);
            if (predicate != null) {
                state = predicate.apply(stack, ref, entity);
            }
            if (state != TriState.DEFAULT) {
                break;
            }
        }
        return state.get();
    }

    static {
        TagKey<Item> all = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("trinkets", "all"));
        PREDICATES.put(ResourceLocation.fromNamespaceAndPath("trinkets", "all"), (stack, ref, entity) -> TriState.TRUE);
        PREDICATES.put(ResourceLocation.fromNamespaceAndPath("trinkets", "none"), (stack, ref, entity) -> TriState.FALSE);
        PREDICATES.put(ResourceLocation.fromNamespaceAndPath("trinkets", "tag"), (stack, ref, entity) -> {
            SlotType slot = ref.inventory().getSlotType();
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("trinkets", slot.getId()));
            if (stack.is(tag) || stack.is(all)) {
                return TriState.TRUE;
            }
            return TriState.DEFAULT;
        });
    }
}
