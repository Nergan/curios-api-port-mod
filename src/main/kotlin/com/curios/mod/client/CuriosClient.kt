package com.curios.mod.client

import com.curios.mod.CuriosMod
import com.curios.mod.CuriosMenu
import com.curios.mod.OpenCuriosPayload
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
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
    }
}

class CuriosScreen(menu: CuriosMenu, inventory: Inventory, title: Component) :
    AbstractContainerScreen<CuriosMenu>(menu, inventory, title) {
    init {
        imageHeight = menu.panelHeight()
        imageWidth = 176
        inventoryLabelY = imageHeight - 94
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val left = (width - imageWidth) / 2
        val top = (height - imageHeight) / 2
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xC0101010.toInt())
        graphics.fill(left + 7, top + 17, left + 169, top + imageHeight - 83, 0xFF2B2B2B.toInt())
    }
}
