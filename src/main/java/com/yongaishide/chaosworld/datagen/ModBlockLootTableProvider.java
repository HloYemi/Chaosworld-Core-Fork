package com.yongaishide.chaosworld.datagen;

import com.yongaishide.chaosworld.metal.ModMetals;
import com.yongaishide.chaosworld.metal.ModTech;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModBlockLootTableProvider extends BlockLootSubProvider {

    protected ModBlockLootTableProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    @Override
    protected void generate() {
        ModMetals.METAL_BLOCKS.values().forEach(holder -> this.dropSelf(holder.get()));
        ModTech.TECH_BLOCKS.values().forEach(holder -> this.dropSelf(holder.get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.concat(
                ModMetals.METAL_BLOCKS.values().stream().map(Holder::value),
                ModTech.TECH_BLOCKS.values().stream().map(Holder::value)
        ).collect(Collectors.toSet());
    }
}
