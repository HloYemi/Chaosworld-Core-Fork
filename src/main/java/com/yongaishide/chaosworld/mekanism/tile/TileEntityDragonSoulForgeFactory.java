package com.yongaishide.chaosworld.mekanism.tile;

import com.jerry.mekextras.api.ExtraUpgrade;
import mekanism.api.Upgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 龙魂锻炉工厂方块实体:逻辑与电路组装工厂一致——每 tick 可处理多份锻炉配方。
 * <p>
 * 等级从放置时的方块状态读取(MEK 4 级 + mekextras 4 级),等级决定每次处理份数。
 * 堆叠升级(mekextras STACK)按 2^N 倍提升并行处理份数与槽位容量。
 */
public class TileEntityDragonSoulForgeFactory extends TileEntityDragonSoulForge {

    private final int factoryProcesses;

    public TileEntityDragonSoulForgeFactory(BlockPos pos, BlockState state) {
        //blockProvider 必须是当前方块本身,MEK 的 tile 用它校验方块实体类型
        super(state.getBlock().builtInRegistryHolder(), pos, state);
        this.factoryProcesses = AssemblingFactoryTiers.getProcesses(state);
    }

    /**
     * 堆叠升级的倍率:2^升级数(mekextras 的 STACK 效果)。
     * 注意:基类构造链(getInitialInventory)中 upgradeComponent 尚未初始化,此时返回 1。
     */
    private int getStackMultiplier() {
        Upgrade stack = ExtraUpgrade.STACK;
        if (stack == null || upgradeComponent == null) {
            return 1;
        }
        return (int) Math.pow(2, upgradeComponent.getUpgrades(stack));
    }

    @Override
    public int getOperationsPerTick() {
        return factoryProcesses * getStackMultiplier();
    }

    @Override
    protected int getGridSlotCapacity() {
        //槽位堆叠上限 = 64 × 等级处理份数 × 堆叠倍率
        int processes = AssemblingFactoryTiers.getProcesses(getBlockState());
        return 64 * processes * getStackMultiplier();
    }

    @Override
    protected int getProcessCount() {
        //流体罐容量倍率 = 等级处理份数(单机为 1)
        return AssemblingFactoryTiers.getProcesses(getBlockState());
    }

    @Override
    protected int getStackLimitShift() {
        return AssemblingFactoryTiers.getStackShift(getBlockState()) + getStackUpgradeExponent();
    }

    /**
     * 每线程一个输出槽:额外列式槽 = 线程数 - 1(主槽 + N-1 个列槽 = N 格产出)。
     */
    @Override
    protected int getExtraOutputSlotCount() {
        return Math.max(0, AssemblingFactoryTiers.getProcesses(getBlockState()) - 1);
    }

    /**
     * 每线程一个输入格:输入槽 = 线程数(3/5/7/9/11/13/15/17)。
     */
    @Override
    protected int getInputSlotCount() {
        return Math.max(1, AssemblingFactoryTiers.getProcesses(getBlockState()));
    }

    /** 堆叠升级数(ExtraUpgrade.STACK),构造早期返回 0 */
    private int getStackUpgradeExponent() {
        Upgrade stack = ExtraUpgrade.STACK;
        if (stack == null || upgradeComponent == null) {
            return 0;
        }
        return upgradeComponent.getUpgrades(stack);
    }
}
