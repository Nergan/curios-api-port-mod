package dev.emi.trinkets.api.client;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class TrinketRendererRegistry {
    private static final Map<Item, TrinketRenderer> RENDERERS = new HashMap<>();

    private TrinketRendererRegistry() {
    }

    public static void registerRenderer(Item item, TrinketRenderer renderer) {
        RENDERERS.put(item, renderer);
    }

    public static Optional<TrinketRenderer> getRenderer(Item item) {
        return Optional.ofNullable(RENDERERS.get(item));
    }
}
