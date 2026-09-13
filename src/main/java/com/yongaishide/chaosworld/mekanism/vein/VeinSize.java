package com.yongaishide.chaosworld.mekanism.vein;

/**
 * 矿脉大小等级(陨石碎片/陨石/小行星/行星/恒星)。
 * 每级对应质量范围(数据包 vein_sizes.json 配置)。
 */
public enum VeinSize {
    METEORITE_FRAGMENT(0),
    METEORITE(1),
    ASTEROID(2),
    PLANET(3),
    STAR(4),
    NEBULA(5),
    GALAXY(6);

    private final int ordinal;
    private static final VeinSize[] BY_ID = values();

    VeinSize(int ordinal) {
        this.ordinal = ordinal;
    }

    public int getId() {
        return ordinal;
    }

    public static VeinSize byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : METEORITE_FRAGMENT;
    }

    public String getName() {
        return name().toLowerCase();
    }
}
