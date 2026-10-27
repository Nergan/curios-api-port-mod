package com.curios.mod.client

import com.curios.mod.CuriosMod
import com.curios.mod.CuriosRuntime
import com.curios.mod.CuriosTrinkets
import com.curios.mod.entityTags
import dev.emi.trinkets.api.client.TrinketRendererRegistry
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext

class CurioFeatureRenderer(
    parent: RenderLayerParent<LivingEntity, EntityModel<LivingEntity>>,
) : RenderLayer<LivingEntity, EntityModel<LivingEntity>>(parent) {
    override fun render(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: LivingEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTick: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
    ) {
        if (entity.isInvisible) return
        val inventory = entity.getAttached(CuriosMod.INVENTORY) ?: return
        val refs = CuriosRuntime.slotsFor(entity.type, entityTags(entity.type))
        val renderer = Minecraft.getInstance().itemRenderer
        for ((index, ref) in refs.withIndex()) {
            val stack = inventory.stack(ref)
            if (stack.isEmpty) continue
            val custom = TrinketRendererRegistry.getRenderer(stack.item)
            if (custom.isPresent) {
                custom.get().render(
                    stack,
                    CuriosTrinkets.reference(entity, ref),
                    parentModel,
                    poseStack,
                    buffer,
                    packedLight,
                    entity,
                    limbSwing,
                    limbSwingAmount,
                    partialTick,
                    ageInTicks,
                    netHeadYaw,
                    headPitch,
                )
                continue
            }
            poseStack.pushPose()
            place(entity, ref.id, ref.cosmetic, poseStack)
            renderer.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.id * 31 + index,
            )
            poseStack.popPose()
        }
    }

    private fun place(entity: LivingEntity, slotId: String, cosmetic: Boolean, poseStack: PoseStack) {
        val model = parentModel
        if (model is HumanoidModel<*>) {
            if (slotId == "head") model.head.translateAndRotate(poseStack)
            else model.body.translateAndRotate(poseStack)
        } else {
            poseStack.translate(0.0, if (entity.isBaby) 0.6 else 1.1, 0.0)
        }
        val (x, y, z, scale) = offset(slotId)
        val side = if (cosmetic) 0.08 else 0.0
        poseStack.translate(x + side, y, z)
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f))
        poseStack.scale(scale, scale, scale)
    }

    private fun offset(slotId: String): SlotOffset = when (slotId) {
        "head" -> SlotOffset(0.0, -0.25, 0.05, 0.45f)
        "necklace" -> SlotOffset(0.0, 0.18, -0.12, 0.42f)
        "back" -> SlotOffset(0.0, 0.22, 0.16, 0.45f)
        "belt" -> SlotOffset(0.0, 0.55, -0.08, 0.32f)
        "ring", "bracelet", "hands" -> SlotOffset(0.28, 0.45, 0.0, 0.22f)
        else -> SlotOffset(0.0, 0.28, -0.1, 0.36f)
    }
}

private data class SlotOffset(val x: Double, val y: Double, val z: Double, val scale: Float)
