package top.theillusivec4.curios.api.type.capability

import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler

/**
 * Shape that LambDynamicLights looks up by name.
 * The port's own handler stays on [top.theillusivec4.curios.api.ICuriosItemHandler].
 * One runtime object implements both.
 */
interface ICuriosItemHandler {
    fun getCurios(): Map<String, ICurioStacksHandler>
}
