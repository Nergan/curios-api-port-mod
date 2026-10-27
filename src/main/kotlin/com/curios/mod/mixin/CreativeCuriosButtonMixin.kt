package com.curios.mod.mixin

import com.curios.mod.client.CuriosButton
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(CreativeModeInventoryScreen::class)
abstract class CreativeCuriosButtonMixin {
    @Inject(method = ["method_25426"], at = [At("TAIL")], remap = false)
    private fun addCuriosButton(callback: CallbackInfo) {
        (this as Screen).addRenderableWidget(CuriosButton(this as AbstractContainerScreen<*>))
    }
}
