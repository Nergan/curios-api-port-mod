package top.theillusivec4.curios.api

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import top.theillusivec4.curios.api.type.capability.ICurio
import java.util.Optional

interface ICuriosItemHandler {
    fun equipped(): List<ItemStack>
    fun isEquipped(predicate: (ItemStack) -> Boolean): Boolean
}

object CuriosApi {
    private var inventoryOf: (LivingEntity) -> Optional<ICuriosItemHandler> = { Optional.empty() }
    private var curioOf: (ItemStack) -> ICurio? = { null }

    @JvmStatic
    fun getCuriosInventory(entity: LivingEntity): Optional<ICuriosItemHandler> = inventoryOf(entity)

    @JvmStatic
    fun getCurio(stack: ItemStack): Optional<ICurio> = Optional.ofNullable(curioOf(stack))

    fun bind(inventoryOf: (LivingEntity) -> Optional<ICuriosItemHandler>, curioOf: (ItemStack) -> ICurio?) {
        this.inventoryOf = inventoryOf
        this.curioOf = curioOf
    }
}
