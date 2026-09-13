package com.yongaishide.chaosworld.metal;

import com.yongaishide.chaosworld.ChaosWorld;
import com.yongaishide.chaosworld.item.BaseItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ModMetals {
    public static final Map<String, DeferredHolder<Item, ? extends Item>> METAL_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Block, ? extends Block>> METAL_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, ? extends Item>> METAL_BLOCK_ITEMS = new LinkedHashMap<>();

    public static final String[][] METALS = {
        {"ice", "\u51B0\u96EA"},
        {"twilight_alloy", "\u66AE\u8272\u5408\u91D1"},
        {"lich", "\u5DEB\u795E"},
        {"chaotic_metal", "\u6DF7\u6C8C\u91D1\u5C5E"},
        {"draconic_metal", "\u795E\u9F99\u91D1\u5C5E"},
        {"wyvern_metal", "\u98DE\u9F99\u91D1\u5C5E"},
        {"atmium", "\u5168\u80FD\u5408\u91D1"},
        {"quantum", "\u91CF\u5B50"},
        {"stainless_steel", "\u4E0D\u9508\u94A2"},
        {"xuancai", "\u70AB\u5F69"},
    };

    public static final String[][] TYPES = {
        {"ingot", "\u952D"},
        {"nugget", "\u7C92"},
        {"plate", "\u677F"},
        {"dust", "\u7C89"},
        {"gear", "\u9F7F\u8F6E"},
        {"rod", "\u68D2"},
    };

    public static final Set<String> NO_TINT_METALS = Set.of("chaotic_metal", "draconic_metal", "wyvern_metal", "xuancai");

    private static final Set<String> CUSTOM_TEXTURE_ITEMS = Set.of(
        "chaotic_metal_ingot", "chaotic_metal_nugget", "chaotic_metal_gear", "chaotic_metal_dust",
        "draconic_metal_ingot", "draconic_metal_nugget", "draconic_metal_gear", "draconic_metal_dust",
        "wyvern_metal_ingot", "wyvern_metal_nugget", "wyvern_metal_gear", "wyvern_metal_dust",
        "xuancai_block", "xuancai_ingot", "xuancai_nugget", "xuancai_plate", "xuancai_dust", "xuancai_gear", "xuancai_rod");

    public static final Map<String, Integer> METAL_COLORS = new HashMap<>();
    static {
        METAL_COLORS.put("ice", 0xFF07e5f5);
        METAL_COLORS.put("twilight_alloy", 0xFFac08e2);
        METAL_COLORS.put("lich", 0xFFd69404);
        METAL_COLORS.put("chaotic_metal", 0xFF1b191b);
        METAL_COLORS.put("draconic_metal", 0xFFe06b04);
        METAL_COLORS.put("wyvern_metal", 0xFF5A377B);
        METAL_COLORS.put("atmium", 0xFF7b0b0b);
        METAL_COLORS.put("quantum", 0xFF00ff48);
        METAL_COLORS.put("stainless_steel", 0xFF8d8b8b);
        METAL_COLORS.put("holy_gold", 0xFFD4AF37);
    }

    public static int getColorForItem(String path) {
        if (path.endsWith("_block")) {
            String metal = path.substring(0, path.length() - 6);
            if (NO_TINT_METALS.contains(metal) && hasCustomTexture(metal, "block")) return 0xFFFFFFFF;
            return METAL_COLORS.getOrDefault(metal, 0xFFFFFFFF);
        }
        for (String[] metal : METALS) {
            if (path.endsWith("_" + metal[0])) {
                String type = path.substring(0, path.length() - metal[0].length() - 1);
                if (NO_TINT_METALS.contains(metal[0]) && hasCustomTexture(metal[0], type)) return 0xFFFFFFFF;
                return METAL_COLORS.getOrDefault(metal[0], 0xFFFFFFFF);
            }
        }
        for (String metal : METAL_COLORS.keySet()) {
            if (path.endsWith("_" + metal)) {
                String type = path.substring(0, path.length() - metal.length() - 1);
                if (NO_TINT_METALS.contains(metal) && hasCustomTexture(metal, type)) return 0xFFFFFFFF;
                return METAL_COLORS.get(metal);
            }
        }
        return 0xFFFFFFFF;
    }

    private static boolean hasCustomTexture(String metal, String type) {
        return CUSTOM_TEXTURE_ITEMS.contains(metal + "_" + type);
    }

    public static void register() {
        for (String[] metal : METALS) {
            String mname = metal[0];
            for (String[] type : TYPES) {
                String suffix = type[0];
                String itemId = suffix + "_" + mname;
                DeferredHolder<Item, ? extends Item> item = ChaosWorld.ITEMS.register(itemId,
                    () -> new BaseItem(new Item.Properties(), false));
                METAL_ITEMS.put(itemId, item);
            }

            String blockId = mname + "_block";
            DeferredHolder<Block, ? extends Block> block = ChaosWorld.BLOCKS.register(blockId,
                () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.METAL)));
            METAL_BLOCKS.put(blockId, block);

            DeferredHolder<Item, ? extends Item> blockItem = ChaosWorld.ITEMS.register(blockId,
                () -> new BlockItem(block.get(), new Item.Properties()));
            METAL_BLOCK_ITEMS.put(blockId, blockItem);
        }

        registerHolyGold();
    }

    private static void registerHolyGold() {
        DeferredHolder<Item, ? extends Item> ingot = ChaosWorld.ITEMS.register("holy_gold_ingot",
            () -> new BaseItem(new Item.Properties(), false));
        METAL_ITEMS.put("holy_gold_ingot", ingot);

        for (String[] type : TYPES) {
            String suffix = type[0];
            if (suffix.equals("ingot")) continue;
            String itemId = suffix + "_holy_gold";
            DeferredHolder<Item, ? extends Item> item = ChaosWorld.ITEMS.register(itemId,
                () -> new BaseItem(new Item.Properties(), false));
            METAL_ITEMS.put(itemId, item);
        }

        String blockId = "holy_gold_block";
        DeferredHolder<Block, ? extends Block> block = ChaosWorld.BLOCKS.register(blockId,
            () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(5.0f, 6.0f)
                .sound(SoundType.METAL)));
        METAL_BLOCKS.put(blockId, block);

        DeferredHolder<Item, ? extends Item> blockItem = ChaosWorld.ITEMS.register(blockId,
            () -> new BlockItem(block.get(), new Item.Properties()));
        METAL_BLOCK_ITEMS.put(blockId, blockItem);
    }
}
