package dev.emi.trinkets.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.trinkets.api.SlotReference;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketRenderer {
    void render(
            ItemStack stack,
            SlotReference slotReference,
            EntityModel<? extends LivingEntity> contextModel,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            LivingEntity entity,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float animationProgress,
            float headYaw,
            float headPitch
    );
}
