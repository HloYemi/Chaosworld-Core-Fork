package com.yongaishide.chaosworld.mixin.ufo;

import appeng.items.materials.UpgradeCardItem;
import com.raishxn.ufo.item.custom.BaseCatalystItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Patch layer for the "UFO Future reset": catalyst rebalance + tooltip that
 * matches the actual effect values (fixes the effect/description mismatch).
 */
@Mixin(value = BaseCatalystItem.class, remap = false)
public abstract class BaseCatalystItemMixin extends UpgradeCardItem {

    public BaseCatalystItemMixin(Properties properties) {
        super(properties);
    }

    @Shadow(remap = false)
    protected String family;

    @Shadow(remap = false)
    protected int tier;

    @Inject(method = "getStaticHeat", at = @At("HEAD"), cancellable = true, remap = false)
    private void chaosworld$patchedHeat(CallbackInfoReturnable<Integer> cir) {
        int h = 0;
        switch (family) {
            case "matterflow":
                if (tier == 1) h = 50;
                if (tier == 2) h = 100;
                if (tier == 3) h = 200;
                break;
            case "chrono":
                if (tier == 1) h = 25;
                if (tier == 2) h = 75;
                if (tier == 3) h = 150;
                break;
            case "overflux":
                h = 0;
                break;
            case "quantum":
                if (tier == 1) h = 75;
                if (tier == 2) h = 150;
                if (tier == 3) h = 300;
                break;
            default:
                return;
        }
        cir.setReturnValue(h);
    }

    @Inject(method = "getPowerMultiplier", at = @At("HEAD"), cancellable = true, remap = false)
    private void chaosworld$patchedPower(CallbackInfoReturnable<Double> cir) {
        if ("matterflow".equals(family)) {
            double baseStat = -10.0;
            double tierMultiplier = 1.0;
            if (tier == 2) tierMultiplier = 2.0;
            if (tier == 3) tierMultiplier = 3.0;
            cir.setReturnValue(Math.max(0.01, 1.0 + (baseStat * tierMultiplier / 100.0)));
        }
    }

    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true, remap = false)
    private void chaosworld$patchedTooltip(ItemStack stack, TooltipContext context, List<Component> components,
            TooltipFlag flag, CallbackInfo ci) {
        if (Screen.hasShiftDown()) {
            double baseStat = 0;
            String statKey = null;
            double tierMultiplier = 1.0;
            int staticHeat = 0;

            if (tier == 2) tierMultiplier = 2.5;
            if (tier == 3) tierMultiplier = 5.0;
            if ("matterflow".equals(family)) {
                if (tier == 2) tierMultiplier = 2.0;
                if (tier == 3) tierMultiplier = 3.0;
            }

            switch (family) {
                case "matterflow" -> {
                    baseStat = -10.0;
                    statKey = "tooltip.ufo.catalyst.stat.energy_cost";
                    if (tier == 1) staticHeat = 50;
                    if (tier == 2) staticHeat = 100;
                    if (tier == 3) staticHeat = 200;
                }
                case "chrono" -> {
                    baseStat = 25.0;
                    statKey = "tooltip.ufo.catalyst.stat.speed";
                    if (tier == 1) staticHeat = 25;
                    if (tier == 2) staticHeat = 75;
                    if (tier == 3) staticHeat = 150;
                }
                case "overflux" -> {
                    baseStat = -10.0;
                    statKey = "tooltip.ufo.catalyst.stat.failure_chance";
                    staticHeat = 0;
                }
                case "quantum" -> {
                    baseStat = 10.0;
                    statKey = "tooltip.ufo.catalyst.stat.bonus_drop";
                    if (tier == 1) staticHeat = 75;
                    if (tier == 2) staticHeat = 150;
                    if (tier == 3) staticHeat = 300;
                }
                default -> {
                    return;
                }
            }

            String familyName = Component.translatable("tooltip.ufo.catalyst.family." + family).getString();
            components.add(Component.translatable("tooltip.ufo.catalyst.family", familyName).withStyle(ChatFormatting.GRAY));
            components.add(Component.translatable("tooltip.ufo.catalyst.tier", tier).withStyle(ChatFormatting.GRAY));
            components.add(Component.empty());

            double finalStat = baseStat * tierMultiplier;
            String sign = finalStat > 0 ? "+" : "";
            ChatFormatting statColor = finalStat > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
            String statName = Component.translatable(statKey).getString();
            String statText = String.format("%s%.1f%% %s", sign, finalStat, statName);
            components.add(Component.translatable("tooltip.ufo.catalyst.effect", statText).withStyle(statColor));

            double heatMult = 1.0 + Math.max(0, staticHeat / 100.0);
            String heatColorFormat = heatMult > 1.0 ? "§c" : "§b";
            String heatName = Component.translatable("tooltip.ufo.catalyst.heat_production").getString();
            String heatText = String.format("%sx%.1f %s", heatColorFormat, heatMult, heatName);
            components.add(Component.translatable("tooltip.ufo.catalyst.thermal", heatText).withStyle(ChatFormatting.GRAY));

            components.add(Component.empty());
            components.add(Component.translatable("tooltip.ufo.catalyst.stacking").withStyle(ChatFormatting.GOLD));
            components.add(Component.translatable("tooltip.ufo.catalyst.stack_format", "2", "175").withStyle(ChatFormatting.GRAY));
            components.add(Component.translatable("tooltip.ufo.catalyst.stack_format", "3", "225").withStyle(ChatFormatting.GRAY));
            components.add(Component.translatable("tooltip.ufo.catalyst.stack_format", "4", "250").withStyle(ChatFormatting.GRAY));
        } else {
            components.add(Component.translatable("tooltip.ufo.press_shift").withStyle(ChatFormatting.DARK_GRAY));
        }

        super.appendHoverText(stack, context, components, flag);
        ci.cancel();
    }
}
