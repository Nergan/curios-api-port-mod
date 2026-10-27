package com.curios.mod.client

import com.curios.mod.mixin.ContainerScreenAccess
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.ImageButton
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

private val CURIOS_BUTTON = WidgetSprites(
    ResourceLocation.fromNamespaceAndPath("curios", "button"),
    ResourceLocation.fromNamespaceAndPath("curios", "button_highlighted"),
)

/**
 * The original Curios control: a 10×10 icon at the top-left of the inventory.
 * Survival sits at gui + (28, 10). Creative inventory tab sits at gui + (75, 8).
 */
class CuriosButton(private val parent: AbstractContainerScreen<*>) : ImageButton(
    0,
    0,
    10,
    10,
    CURIOS_BUTTON,
    { _ -> openFrom(parent) },
    Component.translatable("curios.screen"),
) {
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val creative = parent as? CreativeModeInventoryScreen
        if (creative != null && !creative.isInventoryOpen) {
            active = false
            return
        }
        active = true
        val access = parent as ContainerScreenAccess
        val onCreative = creative != null
        x = access.getCuriosLeft() + if (onCreative) 75 else 28
        y = access.getCuriosTop() + if (onCreative) 8 else 10
        super.renderWidget(graphics, mouseX, mouseY, partialTick)
    }
}

private fun openFrom(parent: AbstractContainerScreen<*>) {
    val book = (parent as? RecipeUpdateListener)?.recipeBookComponent
    if (book != null && book.isVisible) book.toggleVisibility()
    openCuriosScreen()
}
