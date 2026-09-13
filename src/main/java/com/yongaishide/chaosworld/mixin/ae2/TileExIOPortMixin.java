package com.yongaishide.chaosworld.mixin.ae2;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.glodblock.github.extendedae.common.tileentities.TileExIOPort", remap = false)
public abstract class TileExIOPortMixin {

    private static final long BASE = Integer.MAX_VALUE; // 基础速度：每tick转移物品数
    private static final long RAMP_TICKS = 20L;         // 连续工作每20tick翻倍
    private static final int MAX_RAMP = 10;             // 最高×1024

    @Unique
    private long chaosworld_rampLevel = 0L;

    @Inject(method = "tickingRequest", at = @At("HEAD"), remap = false)
    private void chaosworld_ramp(IGridNode node, int ticksSinceLastCall, CallbackInfoReturnable<TickRateModulation> cir) {
        if (!node.isActive() || ticksSinceLastCall > 100) {
            this.chaosworld_rampLevel = 0L;
        } else {
            this.chaosworld_rampLevel++;
        }
    }

    @ModifyConstant(method = "tickingRequest", constant = @Constant(longValue = 2048L), remap = false)
    private long chaosworld_exIOPortSpeed(long original) {
        long ramp = 1L << Math.min(this.chaosworld_rampLevel / RAMP_TICKS, MAX_RAMP);
        return BASE * ramp;
    }
}
