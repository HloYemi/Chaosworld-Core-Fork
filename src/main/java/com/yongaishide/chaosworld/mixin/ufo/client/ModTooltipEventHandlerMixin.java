package com.yongaishide.chaosworld.mixin.ufo.client;

import com.raishxn.ufo.event.ModTooltipEventHandler;
import com.raishxn.ufo.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Patch layer: extra coolant / creative energy cell tooltips. */
@Mixin(value = ModTooltipEventHandler.class, remap = false)
public abstract class ModTooltipEventHandlerMixin {

    @Inject(method = "onTooltip", at = @At("TAIL"), remap = false)
    private static void chaosworld$extraTooltips(ItemTooltipEvent event, CallbackInfo ci) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItems.GELID_CRYOTHEUM_BUCKET.get())) {
            event.getToolTip().add(Component.translatable("tooltip.ufo.gelid_cryotheum.info").withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("tooltip.ufo.coolant.machine_hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (stack.is(ModItems.TEMPORAL_FLUID_BUCKET.get())) {
            event.getToolTip().add(Component.translatable("tooltip.ufo.temporal_fluid.info").withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("tooltip.ufo.coolant.machine_hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (stack.is(Items.WATER_BUCKET)) {
            event.getToolTip().add(Component.translatable("tooltip.ufo.water_coolant.info").withStyle(ChatFormatting.GRAY));
        } else if (stack.is(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ae2", "creative_energy_cell")))) {
            event.getToolTip().add(Component.translatable("tooltip.ufo.creative_energy_cell.header").withStyle(ChatFormatting.LIGHT_PURPLE));
            event.getToolTip().add(Component.translatable("tooltip.ufo.creative_energy_cell.desc").withStyle(ChatFormatting.GRAY));
        }
    }
}
