package com.curios.mod.mixin

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(AbstractContainerScreen::class)
interface ContainerScreenAccess {
    @Accessor("leftPos")
    fun getCuriosLeft(): Int

    @Accessor("leftPos")
    fun setCuriosLeft(value: Int)

    @Accessor("topPos")
    fun getCuriosTop(): Int

    @Accessor("topPos")
    fun setCuriosTop(value: Int)

    @Accessor("imageHeight")
    fun getCuriosImageHeight(): Int

    @Accessor("menu")
    fun getCuriosMenu(): AbstractContainerMenu
}
