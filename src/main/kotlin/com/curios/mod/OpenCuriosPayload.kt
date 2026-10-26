package com.curios.mod

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

object OpenCuriosPayload : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    val TYPE: CustomPacketPayload.Type<OpenCuriosPayload> =
        CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath("curios", "open"))

    val CODEC: StreamCodec<RegistryFriendlyByteBuf, OpenCuriosPayload> = StreamCodec.unit(OpenCuriosPayload)
}
