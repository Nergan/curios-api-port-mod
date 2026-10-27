package com.curios.mod.mixin

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(AbstractContainerScreen::class)
interface ContainerScreenAccess {
    @Accessor("field_2776", remap = false)
    fun getCuriosLeft(): Int

    @Accessor("field_2776", remap = false)
    fun setCuriosLeft(value: Int)

    @Accessor("field_2800", remap = false)
    fun getCuriosTop(): Int

    @Accessor("field_2800", remap = false)
    fun setCuriosTop(value: Int)

    @Accessor("field_2779", remap = false)
    fun getCuriosImageHeight(): Int

    @Accessor("field_2797", remap = false)
    fun getCuriosMenu(): AbstractContainerMenu
}
