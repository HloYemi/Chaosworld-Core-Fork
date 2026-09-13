package com.yongaishide.chaosworld;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = ChaosWorld.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<Integer> TECH_COLOR_1 = BUILDER
            .comment("Color for tech_1 items (hex ARGB)")
            .define("techColor_1", 0xFFe07802);
    private static final ModConfigSpec.ConfigValue<Integer> TECH_COLOR_2 = BUILDER
            .comment("Color for tech_2 items (hex ARGB)")
            .define("techColor_2", 0xFF6c4646);
    private static final ModConfigSpec.ConfigValue<Integer> TECH_COLOR_3 = BUILDER
            .comment("Color for tech_3 items (hex ARGB)")
            .define("techColor_3", 0xFF032F34);
    private static final ModConfigSpec.ConfigValue<Integer> TECH_COLOR_4 = BUILDER
            .comment("Color for tech_4 items (hex ARGB)")
            .define("techColor_4", 0xFF03DE6D);
    private static final ModConfigSpec.ConfigValue<Integer> TECH_COLOR_5 = BUILDER
            .comment("Color for tech_5 items (hex ARGB)")
            .define("techColor_5", 0xFF934FA8);
    private static final ModConfigSpec.ConfigValue<Integer> TECH_COLOR_6 = BUILDER
            .comment("Color for tech_6 items (hex ARGB)")
            .define("techColor_6", 0xFF8B5CF6);
    private static final ModConfigSpec.ConfigValue<Integer> TECH_COLOR_7 = BUILDER
            .comment("Color for tech_7 items (hex ARGB)")
            .define("techColor_7", 0xFFEC4899);
    private static final ModConfigSpec.ConfigValue<Integer> TECH_COLOR_8 = BUILDER
            .comment("Color for tech_8 items (hex ARGB)")
            .define("techColor_8", 0xFFF59E0B);
    private static final ModConfigSpec.ConfigValue<Integer> TECH_COLOR_9 = BUILDER
            .comment("Color for tech_9 items (hex ARGB)")
            .define("techColor_9", 0xFFEF4444);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int[] techColors = new int[9];

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        techColors[0] = TECH_COLOR_1.get();
        techColors[1] = TECH_COLOR_2.get();
        techColors[2] = TECH_COLOR_3.get();
        techColors[3] = TECH_COLOR_4.get();
        techColors[4] = TECH_COLOR_5.get();
        techColors[5] = TECH_COLOR_6.get();
        techColors[6] = TECH_COLOR_7.get();
        techColors[7] = TECH_COLOR_8.get();
        techColors[8] = TECH_COLOR_9.get();
    }
}
