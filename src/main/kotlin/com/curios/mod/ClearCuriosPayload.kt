package com.curios.mod

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

object ClearCuriosPayload : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    val TYPE: CustomPacketPayload.Type<ClearCuriosPayload> =
        CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath("curios", "clear"))

    val CODEC: StreamCodec<RegistryFriendlyByteBuf, ClearCuriosPayload> = StreamCodec.unit(ClearCuriosPayload)
}
