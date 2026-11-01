package com.curios.mod.mixin;

import com.curios.mod.ClearCuriosPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeDestroyCuriosMixin {
    @Shadow
    private Slot destroyItemSlot;

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void clearCurios(Slot slot, int slotId, int button, ClickType action, CallbackInfo callback) {
        if (slot == null || slot != destroyItemSlot || action != ClickType.QUICK_MOVE) {
            return;
        }
        ClientPlayNetworking.send(ClearCuriosPayload.INSTANCE);
    }
}
