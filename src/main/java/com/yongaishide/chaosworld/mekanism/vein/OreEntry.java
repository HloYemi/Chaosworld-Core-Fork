package com.yongaishide.chaosworld.mekanism.vein;

/**
 * 矿石池条目:物品/流体/气体 + 权重 + 质量消耗 + 最低钻头等级。
 * 数据来源:data/chaosworld_core/vein_drill/ore_pools/<维度>.json
 */
public record OreEntry(PoolType type, String id, int weight, long qualityCost, int minDrillLevel) {

    public enum PoolType {
        ITEM,
        FLUID,
        GAS
    }

    public static OreEntry parse(com.google.gson.JsonObject json) {
        String type = json.get("type").getAsString();
        String id = json.get("id").getAsString();
        int weight = json.get("weight").getAsInt();
        long qualityCost = json.get("quality_cost").getAsLong();
        int minDrillLevel = json.has("min_drill_level") ? json.get("min_drill_level").getAsInt() : 1;
        return new OreEntry(PoolType.valueOf(type.toUpperCase()), id, weight, qualityCost, minDrillLevel);
    }
}
