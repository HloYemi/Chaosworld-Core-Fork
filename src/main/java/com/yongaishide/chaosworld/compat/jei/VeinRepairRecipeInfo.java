package com.yongaishide.chaosworld.compat.jei;

import net.minecraft.world.item.ItemStack;

/**
 * 修复品 JEI 条目:一件修复品对应修复量(锤子=钻头以本模组 UFO 锤子图标占位)。
 */
public record VeinRepairRecipeInfo(ItemStack item, int repairAmount) {

    public ItemStack hammer() {
        return new ItemStack(com.raishxn.ufo.item.ModTools.UFO_HAMMER.get());
    }
}
