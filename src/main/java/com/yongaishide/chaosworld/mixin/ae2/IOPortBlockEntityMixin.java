package com.yongaishide.chaosworld.mixin.ae2;

import java.util.ArrayList;
import java.util.List;

import appeng.api.config.Actionable;
import appeng.api.config.OperationMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.storage.cells.StorageCell;
import appeng.api.util.IConfigManager;
import appeng.blockentity.storage.IOPortBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IOPortBlockEntity.class, remap = false)
public abstract class IOPortBlockEntityMixin {

    private static final long BASE = 100000L;     // 基础速度：每tick转移物品数
    private static final long RAMP_TICKS = 20L;   // 连续工作每20tick翻倍
    private static final int MAX_RAMP = 10;       // 最高×1024
    private static final int PARALLEL_LIMIT = 100; // 每tick最多并行处理的物品类型数

    @Unique
    private long chaosworld_rampLevel = 0L;

    @Unique
    private int chaosworld_typeCursor = 0;

    @Shadow(remap = false)
    private IConfigManager manager;

    @Shadow(remap = false)
    private IActionSource mySrc;

    @Inject(method = "tickingRequest", at = @At("HEAD"), remap = false)
    private void chaosworld_ramp(IGridNode node, int ticksSinceLastCall, CallbackInfoReturnable<TickRateModulation> cir) {
        if (!node.isActive() || ticksSinceLastCall > 100) {
            this.chaosworld_rampLevel = 0L;
        } else {
            this.chaosworld_rampLevel++;
        }
    }

    @ModifyConstant(method = "tickingRequest", constant = @Constant(longValue = 256L), remap = false)
    private long chaosworld_ioPortSpeed(long original) {
        long ramp = 1L << Math.min(this.chaosworld_rampLevel / RAMP_TICKS, MAX_RAMP);
        return BASE * ramp;
    }

    @Inject(method = "transferContents", at = @At("HEAD"), cancellable = true, remap = false)
    private void chaosworld_transferContents(IGrid grid, StorageCell cellInv, long itemsToMove, CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(this.chaosworld_parallelTransfer(grid, cellInv, itemsToMove));
    }

    private record ChaosType(AEKey key, long amount) {
    }

    private long chaosworld_parallelTransfer(IGrid grid, StorageCell cellInv, long itemsToMove) {
        var networkInv = grid.getStorageService().getInventory();

        KeyCounter srcList;
        MEStorage src, destination;
        if (this.manager.getSetting(Settings.OPERATION_MODE) == OperationMode.EMPTY) {
            src = cellInv;
            srcList = cellInv.getAvailableStacks();
            destination = networkInv;
        } else {
            src = networkInv;
            srcList = grid.getStorageService().getCachedInventory();
            destination = cellInv;
        }

        var energy = grid.getEnergyService();

        List<AEKey> keys = new ArrayList<>();
        List<Long> amounts = new ArrayList<>();
        for (var srcEntry : srcList) {
            keys.add(srcEntry.getKey());
            amounts.add(srcEntry.getLongValue());
        }

        int size = keys.size();
        if (size == 0) {
            return itemsToMove;
        }

        int cursor = this.chaosworld_typeCursor % size;
        int scanned = 0;
        long moved = 0L;
        while (scanned < PARALLEL_LIMIT && scanned < size) {
            int idx = (cursor + scanned) % size;
            AEKey key = keys.get(idx);
            long amount = amounts.get(idx);
            if (amount > 0) {
                var possible = destination.insert(key, amount, Actionable.SIMULATE, this.mySrc);
                if (possible > 0) {
                    possible = Math.min(possible, itemsToMove * key.getAmountPerOperation());
                    possible = src.extract(key, possible, Actionable.MODULATE, this.mySrc);
                    if (possible > 0) {
                        var inserted = StorageHelper.poweredInsert(energy, destination, key, possible, this.mySrc);
                        if (inserted < possible) {
                            src.insert(key, possible - inserted, Actionable.MODULATE, this.mySrc);
                        }
                        if (inserted > 0) {
                            moved += inserted;
                        }
                    }
                }
            }
            scanned++;
        }

        this.chaosworld_typeCursor = (cursor + scanned) % size;
        return (scanned >= PARALLEL_LIMIT && moved > 0) ? 0L : itemsToMove;
    }
}
