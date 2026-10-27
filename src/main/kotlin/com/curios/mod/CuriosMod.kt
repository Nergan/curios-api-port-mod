package com.curios.mod

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.network.codec.ByteBufCodecs
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.world.InteractionResultHolder
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.PackType
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import dev.emi.trinkets.api.TrinketEnums
import dev.emi.trinkets.api.TrinketItem
import dev.emi.trinkets.api.TrinketsApi
import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.type.capability.ICurio

class CuriosMod : ModInitializer {
    override fun onInitialize() {
        Registry.register(BuiltInRegistries.MENU, id("curios"), MENU)
        PayloadTypeRegistry.playC2S().register(OpenCuriosPayload.TYPE, OpenCuriosPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(ClearCuriosPayload.TYPE, ClearCuriosPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(OpenCuriosPayload.TYPE) { _, context ->
            context.server().execute { open(context.player()) }
        }
        ServerPlayNetworking.registerGlobalReceiver(ClearCuriosPayload.TYPE) { _, context ->
            context.server().execute { clearCurios(context.player()) }
        }
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(CuriosReload())
        ServerTickEvents.END_WORLD_TICK.register { world ->
            for (player in world.players()) tick(player)
        }
        ServerLivingEntityEvents.AFTER_DEATH.register { entity, source ->
            if (entity is ServerPlayer) drop(entity, source)
        }
        CuriosApi.bind(
            inventoryOf = { entity -> optionalHandler(entity.getAttached(INVENTORY)) },
            curioOf = { stack -> stack.item as? ICurio },
        )
        CuriosTrinkets.install()
        UseItemCallback.EVENT.register { player, world, hand ->
            val stack = player.getItemInHand(hand)
            if (!world.isClientSide && TrinketsApi.getTrinket(stack.item).canEquipFromUse(stack, player) && TrinketItem.equipItem(player, stack)) {
                InteractionResultHolder.success(stack)
            } else {
                InteractionResultHolder.pass(stack)
            }
        }
    }

    companion object {
        val INVENTORY: AttachmentType<CuriosInventory> = AttachmentRegistry.create(id("inventory")) { builder ->
            builder.initializer { CuriosInventory() }
                .persistent(CuriosInventory.CODEC)
                .syncWith(ByteBufCodecs.fromCodecWithRegistries(CuriosInventory.CODEC), AttachmentSyncPredicate.all())
        }

        val MENU: ExtendedScreenHandlerType<CuriosMenu, List<SlotRef>> = ExtendedScreenHandlerType(
            { syncId, inventory, layout -> CuriosMenu(syncId, inventory, layout) },
            CuriosLayout.CODEC,
        )

        fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath("curios", path)

        fun inventory(player: Player): CuriosInventory = player.getAttachedOrCreate(INVENTORY)

        fun layout(player: Player): List<SlotRef> = CuriosRuntime.slotsFor(player.type, entityTags(player.type))
    }
}

private fun clearCurios(player: ServerPlayer) {
    if (!player.isCreative) return
    val inventory = player.getAttached(CuriosMod.INVENTORY) ?: return
    for (ref in inventory.storedRefs()) {
        CuriosTrinkets.change(player, ref, ItemStack.EMPTY)
    }
}

private fun open(player: ServerPlayer) {
    val layout = CuriosMod.layout(player)
    val dropped = CuriosMod.inventory(player).ensure(layout)
    for (stack in dropped) player.drop(stack, false)
    player.openMenu(object : ExtendedScreenHandlerFactory<List<SlotRef>> {
        override fun getScreenOpeningData(player: ServerPlayer): List<SlotRef> = layout
        override fun getDisplayName(): Component = Component.translatable("curios.screen")
        override fun createMenu(syncId: Int, inventory: Inventory, player: Player): CuriosMenu =
            CuriosMenu(syncId, inventory, layout)
    })
}

private fun tick(player: ServerPlayer) {
    val inventory = CuriosMod.inventory(player)
    val layout = CuriosMod.layout(player)
    val dropped = inventory.ensure(layout)
    if (dropped.isNotEmpty()) {
        for (stack in dropped) player.drop(stack, false)
        player.setAttached(CuriosMod.INVENTORY, inventory)
    }
    for (ref in layout) {
        val stack = inventory.stack(ref)
        (stack.item as? ICurio)?.curioTick(context(player, ref))
        if (!stack.isEmpty) {
            TrinketsApi.getTrinket(stack.item).tick(stack, CuriosTrinkets.reference(player, ref), player)
        }
    }
}

private fun trinketDropRule(player: ServerPlayer, stack: ItemStack, ref: SlotRef): ICurio.DropRule =
    when (TrinketsApi.getTrinket(stack.item).getDropRule(stack, CuriosTrinkets.reference(player, ref), player)) {
        TrinketEnums.DropRule.DESTROY -> ICurio.DropRule.DESTROY
        TrinketEnums.DropRule.ALWAYS_KEEP -> ICurio.DropRule.ALWAYS_KEEP
        TrinketEnums.DropRule.ALWAYS_DROP -> ICurio.DropRule.ALWAYS_DROP
        TrinketEnums.DropRule.DEFAULT -> ICurio.DropRule.DEFAULT
    }

private fun drop(player: ServerPlayer, source: net.minecraft.world.damagesource.DamageSource) {
    val inventory = player.getAttached(CuriosMod.INVENTORY) ?: return
    val layout = CuriosMod.layout(player)
    val recentlyHit = player.lastHurtByMobTimestamp + 100 >= player.tickCount
    for (ref in layout) {
        val stack = inventory.stack(ref)
        if (stack.isEmpty) continue
        val rule = (stack.item as? ICurio)?.getDropRule(context(player, ref), source, recentlyHit)
            ?: trinketDropRule(player, stack, ref)
        when (rule) {
            ICurio.DropRule.ALWAYS_KEEP -> Unit
            ICurio.DropRule.DESTROY -> inventory.set(ref, ItemStack.EMPTY)
            ICurio.DropRule.DEFAULT, ICurio.DropRule.ALWAYS_DROP -> {
                player.drop(stack.copy(), true, false)
                inventory.set(ref, ItemStack.EMPTY)
            }
        }
    }
    player.setAttached(CuriosMod.INVENTORY, inventory)
}
