package com.yongaishide.chaosworld.datagen;

import com.yongaishide.chaosworld.ChaosWorld;
import com.yongaishide.chaosworld.metal.ModMetals;
import com.yongaishide.chaosworld.metal.ModTech;
import com.yongaishide.chaosworld.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, "chaosworld_core", existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.Items.CATALYST).add(
                ChaosWorld.DRAGON_CATALYST.get(),
                ChaosWorld.TWILIGHT_CATALYST.get()
        );

        addMetalTags();
    }

    private static final Map<String, String> SUFFIX_TO_TAG = Map.of(
            "ingot", "c:ingots",
            "nugget", "c:nuggets",
            "plate", "c:plates",
            "dust", "c:dusts",
            "gear", "c:gears",
            "rod", "c:rods"
    );

    private void addMetalTags() {
        addMetalItemTags(ModMetals.METAL_ITEMS, ModMetals.METAL_BLOCK_ITEMS);
        addMetalItemTags(ModTech.TECH_ITEMS, ModTech.TECH_BLOCK_ITEMS);
        TagKey<Item> holyGoldIngot = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:ingots/holy_gold"));
        if (ModMetals.METAL_ITEMS.get("holy_gold_ingot") != null) {
            tag(holyGoldIngot).add(ModMetals.METAL_ITEMS.get("holy_gold_ingot").get());
            tag(TagKey.create(Registries.ITEM, ResourceLocation.parse("c:ingots"))).add(ModMetals.METAL_ITEMS.get("holy_gold_ingot").get());
        }
    }

    private void addMetalItemTags(Map<String, DeferredHolder<Item, ? extends Item>> items,
                                  Map<String, DeferredHolder<Item, ? extends Item>> blockItems) {
        items.forEach((id, holder) -> {
            for (Map.Entry<String, String> entry : SUFFIX_TO_TAG.entrySet()) {
                if (id.startsWith(entry.getKey() + "_")) {
                    String material = id.substring(entry.getKey().length() + 1);
                    TagKey<Item> specific = TagKey.create(Registries.ITEM,
                            ResourceLocation.parse(entry.getValue() + "/" + material));
                    tag(specific).add(holder.get());
                    tag(TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.getValue()))).add(holder.get());
                }
            }
        });
        blockItems.forEach((id, holder) -> {
            if (id.endsWith("_block")) {
                String material = id.substring(0, id.length() - 6);
                TagKey<Item> specific = TagKey.create(Registries.ITEM,
                        ResourceLocation.parse("c:storage_blocks/" + material));
                tag(specific).add(holder.get());
                tag(TagKey.create(Registries.ITEM, ResourceLocation.parse("c:storage_blocks"))).add(holder.get());
            }
        });
    }
}
