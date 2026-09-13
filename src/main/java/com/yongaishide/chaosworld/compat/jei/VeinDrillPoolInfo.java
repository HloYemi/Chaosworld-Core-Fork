package com.yongaishide.chaosworld.compat.jei;

import java.util.List;
import com.yongaishide.chaosworld.mekanism.vein.OreEntry;
import net.minecraft.resources.ResourceLocation;

/**
 * 虚脉钻探机的一个"配方" = 一个统一池页面(每页最多 16 条产物)。
 * 超过一页的池在注册时拆成多个 page,JEI 自动提供前后翻页箭头。
 */
public record VeinDrillPoolInfo(ResourceLocation dimension, List<OreEntry> entries, int page, int pageCount) {

    public VeinDrillPoolInfo(ResourceLocation dimension, List<OreEntry> entries) {
        this(dimension, entries, 0, 1);
    }

    public int totalEntries() {
        int total = 0;
        for (OreEntry entry : entries) {
            total += entry.weight();
        }
        return total;
    }
}
