package com.curios.mod.mixin

import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/**
 * Sophisticated Backpacks closes the screen when the wrapper object changes.
 * Opening the menu writes components onto the same stack, the cache misses, and a new wrapper appears.
 * The stack in the curios slot is still that same object, so the open backpack has not been replaced.
 */
@Mixin(targets = ["net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer"], remap = false)
abstract class BackpackMenuMixin {
    @Inject(method = ["storageItemHasChanged"], at = [At("RETURN")], cancellable = true, remap = false)
    private fun sameStackIsTheSameBackpack(callback: CallbackInfoReturnable<Boolean>) {
        if (callback.returnValue != true) return
        try {
            val context = declaredField(this, "backpackContext")
            val player = declaredField(this, "player")
            val current = declaredField(this, "storageWrapper")
            val lookup = context.javaClass.methods.firstOrNull { it.name == "getBackpackWrapper" && it.parameterCount == 1 }
                ?: return
            val latest = lookup.invoke(context, player)
            val currentStack = stackOf(current) ?: return
            val latestStack = stackOf(latest) ?: return
            if (currentStack === latestStack) callback.returnValue = false
        } catch (_: ReflectiveOperationException) {
            // Leave the backpack mod's own result in place.
        }
    }
}

private fun stackOf(wrapper: Any): Any? {
    val method = wrapper.javaClass.methods.firstOrNull { it.name == "getBackpack" && it.parameterCount == 0 } ?: return null
    return method.invoke(wrapper)
}

private fun declaredField(target: Any, name: String): Any {
    var type: Class<*>? = target.javaClass
    while (type != null) {
        try {
            val field = type.getDeclaredField(name)
            field.isAccessible = true
            return field.get(target) ?: throw NoSuchFieldException(name)
        } catch (_: NoSuchFieldException) {
            type = type.superclass
        }
    }
    throw NoSuchFieldException(name)
}
