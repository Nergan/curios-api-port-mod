package dev.emi.trinkets.api;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Set;

/**
 * Class, not an interface: Trinkets callers such as Sophisticated Backpacks
 * invoke these getters with invokevirtual.
 */
public class SlotType {
    private final String group;
    private final String name;
    private final int order;
    private final int amount;
    private final ResourceLocation icon;
    private final Set<ResourceLocation> validatorPredicates;
    private final Set<ResourceLocation> quickMovePredicates;
    private final Set<ResourceLocation> tooltipPredicates;
    private final TrinketEnums.DropRule dropRule;

    public SlotType(String group, String name, int order, int amount, ResourceLocation icon,
                    Set<ResourceLocation> validatorPredicates,
                    Set<ResourceLocation> quickMovePredicates,
                    Set<ResourceLocation> tooltipPredicates,
                    TrinketEnums.DropRule dropRule) {
        this.group = group;
        this.name = name;
        this.order = order;
        this.amount = amount;
        this.icon = icon;
        this.validatorPredicates = copy(validatorPredicates);
        this.quickMovePredicates = copy(quickMovePredicates);
        this.tooltipPredicates = copy(tooltipPredicates);
        this.dropRule = dropRule != null ? dropRule : TrinketEnums.DropRule.DEFAULT;
    }

    public SlotType(String group, String name, Set<ResourceLocation> validatorPredicates) {
        this(group, name, 0, 1, ResourceLocation.fromNamespaceAndPath("trinkets", "gui/slots/" + name),
                validatorPredicates,
                Set.of(ResourceLocation.fromNamespaceAndPath("trinkets", "all")),
                Set.of(ResourceLocation.fromNamespaceAndPath("trinkets", "all")),
                TrinketEnums.DropRule.DEFAULT);
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return group + "/" + name;
    }

    public int getOrder() {
        return order;
    }

    public int getAmount() {
        return amount;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public Set<ResourceLocation> getValidatorPredicates() {
        return validatorPredicates;
    }

    public Set<ResourceLocation> getQuickMovePredicates() {
        return quickMovePredicates;
    }

    public Set<ResourceLocation> getTooltipPredicates() {
        return tooltipPredicates;
    }

    public TrinketEnums.DropRule getDropRule() {
        return dropRule;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SlotType slotType)) {
            return false;
        }
        return group.equals(slotType.group) && name.equals(slotType.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name);
    }

    private static Set<ResourceLocation> copy(Set<ResourceLocation> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }
}
