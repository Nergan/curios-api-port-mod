package com.curios.mod

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

object OpenInventoryPayload : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    val TYPE: CustomPacketPayload.Type<OpenInventoryPayload> =
        CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath("curios", "open_inventory"))

    val CODEC: StreamCodec<RegistryFriendlyByteBuf, OpenInventoryPayload> = StreamCodec.unit(OpenInventoryPayload)
}
