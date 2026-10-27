package com.curios.mod.client

import com.curios.mod.CuriosMod
import com.curios.mod.CuriosRuntime
import com.curios.mod.CuriosMenu
import com.curios.mod.OpenCuriosPayload
import com.curios.mod.curioColumns
import com.curios.mod.curioPanelContains
import com.curios.mod.curioPanelHeight
import com.curios.mod.curioPanelWidth
import com.curios.mod.curioPanelX
import com.curios.mod.curioRowCount
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Inventory
import org.lwjgl.glfw.GLFW

class CuriosClient : ClientModInitializer {
    override fun onInitializeClient() {
        CuriosRuntime.loadBundledData()
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
                val screen = client.screen
                val inventoryOpen = screen is InventoryScreen || screen is CreativeModeInventoryScreen
                if (client.player != null && screen !is CuriosScreen && (screen == null || inventoryOpen)) {
                    openCuriosScreen()
                }
            }
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

fun openCuriosScreen() {
    runCatching { ClientPlayNetworking.send(OpenCuriosPayload) }
        .onFailure { curiosClientLog.warn("Could not open the curios menu", it) }
}

private val curiosClientLog = LoggerFactory.getLogger("curios")

class CuriosScreen(menu: CuriosMenu, inventory: Inventory, title: Component) :
    AbstractContainerScreen<CuriosMenu>(menu, inventory, title) {
    init {
        imageWidth = 176
        imageHeight = 166
    }

    override fun hasClickedOutside(mouseX: Double, mouseY: Double, guiLeft: Int, guiTop: Int, mouseButton: Int): Boolean {
        if (!super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton)) return false
        return !curioPanelContains(mouseX, mouseY, guiLeft, guiTop, menu.curioCount)
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.drawString(font, title, 97, 6, 0x404040, false)
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        graphics.blit(INVENTORY_TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256)
        val count = menu.curioCount
        val columns = curioColumns(count)
        val rows = curioRowCount(count)
        if (columns > 0 && rows > 0) {
            val x0 = leftPos + curioPanelX(count)
            val y0 = topPos
            val panelWidth = curioPanelWidth(count)
            val panelHeight = curioPanelHeight(count)
            graphics.fill(x0, y0, x0 + panelWidth, y0 + panelHeight, 0xFFC6C6C6.toInt())
            val innerX = x0 + 7
            val innerY = y0 + 7
            graphics.fill(innerX, innerY, innerX + columns * 18, innerY + rows * 18, 0xFF8B8B8B.toInt())
        }
        val player = minecraft?.player ?: return
        InventoryScreen.renderEntityInInventoryFollowsMouse(
            graphics,
            leftPos + 26,
            topPos + 8,
            leftPos + 75,
            topPos + 78,
            30,
            0.0625f,
            mouseX.toFloat(),
            mouseY.toFloat(),
            player,
        )
    }
}

private val INVENTORY_TEXTURE: ResourceLocation =
    ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png")
