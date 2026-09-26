package com.yongaishide.chaosworld.datagen;

import com.yongaishide.chaosworld.metal.ModMetals;
import com.yongaishide.chaosworld.metal.ModTech;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, "chaosworld_core", existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        ModMetals.METAL_BLOCKS.values().forEach(holder -> {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(holder.get());
            addStorageBlockTag(holder);
        });
        ModTech.TECH_BLOCKS.values().forEach(holder -> {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(holder.get());
            addStorageBlockTag(holder);
        });
    }

    private void addStorageBlockTag(DeferredHolder<Block, ? extends Block> holder) {
        String id = holder.getId().getPath();
        if (id.endsWith("_block")) {
            String material = id.substring(0, id.length() - 6);
            TagKey<Block> specific = TagKey.create(Registries.BLOCK,
                    ResourceLocation.parse("c:storage_blocks/" + material));
            tag(specific).add(holder.get());
            tag(TagKey.create(Registries.BLOCK, ResourceLocation.parse("c:storage_blocks"))).add(holder.get());
        }
    }
}
