package com.yongaishide.chaosworld.mekanism.tile;

import com.jerry.mekextras.api.tier.AdvancedTier;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.yongaishide.chaosworld.mekanism.MekanismMachines;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 机械组装工厂的等级体系:MEK 4 级 + mekextras 4 级,以及每级每次处理的配方份数。
 */
public final class AssemblingFactoryTiers {

    private AssemblingFactoryTiers() {
    }

    /**
     * 占位方块:工厂 tile 构造时作为 blockProvider 使用(等级不从这里读)。
     */
    public static final Holder<Block> PLACEHOLDER_BLOCK = MekanismMachines.MECHANICAL_ASSEMBLER;

    /**
     * 从放置的方块状态读取等级对应的每次处理份数。
     */
    public static int getProcesses(BlockState state) {
        Holder<Block> holder = state.getBlockHolder();
        BaseTier baseTier = Attribute.getBaseTier(holder);
        if (baseTier != null) {
            return switch (baseTier) {
                case BASIC -> 3;
                case ADVANCED -> 5;
                case ELITE -> 7;
                case ULTIMATE -> 9;
                default -> 1;
            };
        }
        AdvancedTier advancedTier = ExtraAttribute.getAdvancedTier(holder);
        if (advancedTier != null) {
            //与 mekextras ExtraFactoryTier 的处理份数一致
            return switch (advancedTier) {
                case ABSOLUTE -> 11;
                case SUPREME -> 13;
                case COSMIC -> 15;
                case INFINITE -> 17;
            };
        }
        return 1;
    }

    /**
     * 堆叠扩容指数(2^shift):工厂等级每级 +1(基本 0 ~ 无限 7),单机为 0。
     * 与 mekextras 堆叠工厂的 2^n 式扩容一致。
     */
    public static int getStackShift(BlockState state) {
        Holder<Block> holder = state.getBlockHolder();
        BaseTier baseTier = Attribute.getBaseTier(holder);
        if (baseTier != null) {
            return switch (baseTier) {
                case BASIC -> 0;
                case ADVANCED -> 1;
                case ELITE -> 2;
                case ULTIMATE -> 3;
                default -> 0;
            };
        }
        AdvancedTier advancedTier = ExtraAttribute.getAdvancedTier(holder);
        if (advancedTier != null) {
            return switch (advancedTier) {
                case ABSOLUTE -> 4;
                case SUPREME -> 5;
                case COSMIC -> 6;
                case INFINITE -> 7;
            };
        }
        return 0;
    }
}
