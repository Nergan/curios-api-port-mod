package top.theillusivec4.curios.api.type.capability

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootContext
import top.theillusivec4.curios.api.SlotContext

interface ICurio {
    fun curioTick(slotContext: SlotContext) {}

    fun onEquip(slotContext: SlotContext, prevStack: ItemStack) {}

    fun onUnequip(slotContext: SlotContext, newStack: ItemStack) {}

    fun canEquip(slotContext: SlotContext): Boolean = true

    fun canUnequip(slotContext: SlotContext): Boolean = true

    fun getSlotsTooltip(tooltips: MutableList<Component>, context: TooltipContext): MutableList<Component> = tooltips

    fun getAttributeModifiers(
        slotContext: SlotContext,
        id: ResourceLocation,
    ): Multimap<Holder<Attribute>, AttributeModifier> = LinkedHashMultimap.create()

    fun onEquipFromUse(slotContext: SlotContext) {}

    fun getEquipSound(slotContext: SlotContext): SoundInfo? = null

    fun canEquipFromUse(slotContext: SlotContext): Boolean = false

    fun curioBreak(slotContext: SlotContext) {}

    fun canSync(slotContext: SlotContext): Boolean = false

    fun writeSyncData(slotContext: SlotContext): CompoundTag = CompoundTag()

    fun readSyncData(slotContext: SlotContext, compound: CompoundTag) {}

    fun getDropRule(slotContext: SlotContext, source: DamageSource, recentlyHit: Boolean): DropRule = DropRule.DEFAULT

    fun getAttributesTooltip(tooltips: MutableList<Component>, context: TooltipContext): MutableList<Component> = tooltips

    fun getFortuneLevel(slotContext: SlotContext, lootContext: LootContext?): Int = 0

    fun getLootingLevel(slotContext: SlotContext, lootContext: LootContext?): Int = 0

    fun makesPiglinsNeutral(slotContext: SlotContext): Boolean = false

    fun canWalkOnPowderedSnow(slotContext: SlotContext): Boolean = false

    fun isEnderMask(slotContext: SlotContext, enderMan: EnderMan): Boolean = false

    enum class DropRule {
        DEFAULT,
        ALWAYS_DROP,
        ALWAYS_KEEP,
        DESTROY,
    }

    data class SoundInfo(val soundEvent: SoundEvent, val volume: Float, val pitch: Float)
}
