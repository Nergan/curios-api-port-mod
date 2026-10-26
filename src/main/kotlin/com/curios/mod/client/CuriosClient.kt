package com.curios.mod.client

import com.curios.mod.CURIO_TOP
import com.curios.mod.CURIO_VISIBLE_ROWS
import com.curios.mod.CuriosMod
import com.curios.mod.CuriosMenu
import com.curios.mod.OpenCuriosPayload
import com.curios.mod.curioSlotPosition
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Inventory
import org.lwjgl.glfw.GLFW

class CuriosClient : ClientModInitializer {
    override fun onInitializeClient() {
        net.minecraft.client.gui.screens.MenuScreens.register(CuriosMod.MENU, ::CuriosScreen)
        val mapping = KeyBindingHelper.registerKeyBinding(
            KeyMapping(
                "curios.key.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "curios.key.category",
            ),
        )
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (mapping.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    ClientPlayNetworking.send(OpenCuriosPayload)
                }
            }
        }
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen !is InventoryScreen) return@register
            screen.addRenderableWidget(
                Button.builder(Component.translatable("curios.screen")) {
                    ClientPlayNetworking.send(OpenCuriosPayload)
                }.bounds(screen.leftPos + screen.imageWidth + 2, screen.topPos + 4, 70, 20).build(),
            )
        }
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register { _, renderer, helper, _ ->
            if (renderer !is LivingEntityRenderer<*, *>) return@register
            @Suppress("UNCHECKED_CAST")
            val parent = renderer as RenderLayerParent<LivingEntity, EntityModel<LivingEntity>>
            @Suppress("UNCHECKED_CAST")
            helper.register(CurioFeatureRenderer(parent) as RenderLayer<LivingEntity, EntityModel<LivingEntity>>)
        }
    }
}

class CuriosScreen(menu: CuriosMenu, inventory: Inventory, title: Component) :
    AbstractContainerScreen<CuriosMenu>(menu, inventory, title) {
    private var scroll = 0

    init {
        imageHeight = menu.panelHeight()
        imageWidth = 176
        inventoryLabelY = imageHeight - 94
    }

    override fun init() {
        super.init()
        reposition()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val max = (menu.curioCount - CURIO_VISIBLE_ROWS).coerceAtLeast(0)
        if (max == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        val next = (scroll + if (scrollY < 0) 1 else -1).coerceIn(0, max)
        if (next != scroll) {
            scroll = next
            reposition()
        }
        return true
    }

    private fun reposition() {
        for (index in 0 until menu.curioCount) {
            val slot = menu.slots[index]
            val (x, y) = curioSlotPosition(index, scroll)
            slot.x = x
            slot.y = y
        }
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val left = (width - imageWidth) / 2
        val top = (height - imageHeight) / 2
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xC0101010.toInt())
        graphics.fill(left + 7, top + 17, left + 27, top + CURIO_TOP + CURIO_VISIBLE_ROWS * 18, 0xFF2B2B2B.toInt())
        val player = minecraft?.player ?: return
        val previewTop = top + 8
        val previewBottom = top + CURIO_TOP + CURIO_VISIBLE_ROWS * 18
        InventoryScreen.renderEntityInInventoryFollowsMouse(
            graphics,
            left + 32,
            previewTop,
            left + imageWidth - 8,
            previewBottom,
            30,
            0.0625f,
            mouseX.toFloat(),
            mouseY.toFloat(),
            player,
        )
    }
}
