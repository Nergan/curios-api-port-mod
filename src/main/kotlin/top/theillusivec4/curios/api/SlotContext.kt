package top.theillusivec4.curios.api

import net.minecraft.world.entity.LivingEntity

data class SlotContext(
    val identifier: String,
    val entity: LivingEntity?,
    val index: Int,
    val cosmetic: Boolean,
    val visible: Boolean,
)
