package com.curios.mod.client

import com.curios.mod.CuriosMod
import com.curios.mod.CuriosRuntime
import com.curios.mod.CuriosMenu
import com.curios.mod.OpenCuriosPayload
import com.curios.mod.OpenInventoryPayload
import com.curios.mod.curioInventoryPosition
import com.curios.mod.curioPanelContains
import com.curios.mod.curioPanelHeight
import com.curios.mod.curioPanelWidth
import com.curios.mod.curioPanelX
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen
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
import net.minecraft.world.item.ItemStack
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
        curiosKey = mapping
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

/** Mirrors the server side of [OpenInventoryPayload]: the cursor stack moves to the vanilla inventory menu. */
fun returnToInventory() {
    val minecraft = Minecraft.getInstance()
    val player = minecraft.player ?: return
    if (player.containerMenu !is CuriosMenu) return
    val sent = runCatching { ClientPlayNetworking.send(OpenInventoryPayload) }
        .onFailure { curiosClientLog.warn("Could not return to the inventory", it) }
    if (sent.isFailure) return
    val carried = player.containerMenu.carried
    player.containerMenu.carried = ItemStack.EMPTY
    player.clientSideCloseContainer()
    player.inventoryMenu.carried = carried
    minecraft.setScreen(InventoryScreen(player))
}

private val curiosClientLog = LoggerFactory.getLogger("curios")

private lateinit var curiosKey: KeyMapping

class CuriosScreen(menu: CuriosMenu, inventory: Inventory, title: Component) :
    EffectRenderingInventoryScreen<CuriosMenu>(menu, inventory, title) {
    init {
        imageWidth = 176
        imageHeight = 166
    }

    override fun init() {
        super.init()
        addRenderableWidget(CuriosButton(this))
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (curiosKey.matches(keyCode, scanCode)) {
            returnToInventory()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun hasClickedOutside(mouseX: Double, mouseY: Double, guiLeft: Int, guiTop: Int, mouseButton: Int): Boolean {
        if (!super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton)) return false
        return !curioPanelContains(mouseX, mouseY, guiLeft, guiTop, menu.curioCount)
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.drawString(font, title, 97, 6, 0x404040, false)
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val count = menu.curioCount
        val panelWidth = curioPanelWidth(count)
        if (panelWidth > 0) {
            // The right border tucks under the inventory, so the panel reads as part of the same window.
            drawFrame(graphics, leftPos + curioPanelX(count), topPos, panelWidth + FRAME, curioPanelHeight(count))
            for (index in 0 until count) {
                val (x, y) = curioInventoryPosition(index, count)
                if (x > -1000) graphics.blitSprite(SLOT_SPRITE, leftPos + x - 1, topPos + y - 1, 18, 18)
            }
        }
        graphics.blit(INVENTORY_TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256)
        // This menu has no crafting grid; the vanilla one would be slots that take nothing.
        graphics.fill(leftPos + 96, topPos + 16, leftPos + 172, topPos + 60, BACKGROUND)
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
private val SLOT_SPRITE: ResourceLocation = ResourceLocation.withDefaultNamespace("container/slot")
private const val BACKGROUND = 0xFFC6C6C6.toInt()
private const val FRAME = 4
private const val EDGE = 64

/** Vanilla window frame cut from the corners and straight edges of `inventory.png`, so resource packs restyle it too. */
private fun drawFrame(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int) {
    val right = x + width - FRAME
    val bottom = y + height - FRAME
    graphics.blit(INVENTORY_TEXTURE, x, y, 0f, 0f, FRAME, FRAME, 256, 256)
    graphics.blit(INVENTORY_TEXTURE, right, y, 172f, 0f, FRAME, FRAME, 256, 256)
    graphics.blit(INVENTORY_TEXTURE, x, bottom, 0f, 162f, FRAME, FRAME, 256, 256)
    graphics.blit(INVENTORY_TEXTURE, right, bottom, 172f, 162f, FRAME, FRAME, 256, 256)
    for (edgeX in x + FRAME until right step EDGE) {
        val length = minOf(EDGE, right - edgeX)
        graphics.blit(INVENTORY_TEXTURE, edgeX, y, 8f, 0f, length, FRAME, 256, 256)
        graphics.blit(INVENTORY_TEXTURE, edgeX, bottom, 8f, 162f, length, FRAME, 256, 256)
    }
    for (edgeY in y + FRAME until bottom step EDGE) {
        val length = minOf(EDGE, bottom - edgeY)
        graphics.blit(INVENTORY_TEXTURE, x, edgeY, 0f, 8f, FRAME, length, 256, 256)
        graphics.blit(INVENTORY_TEXTURE, right, edgeY, 172f, 60f, FRAME, length, 256, 256)
    }
    graphics.fill(x + FRAME, y + FRAME, right, bottom, BACKGROUND)
}
