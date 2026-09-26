package com.yongaishide.chaosworld.mixin.ufo;

import com.raishxn.ufo.block.MultiblockBlocks;
import com.raishxn.ufo.block.entity.pattern.QuantumPatternPredicates;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Patch layer: structure hint names use the real block/translation names
 * (fixes the garbled auto-build display) and the field predicate accepts any
 * field generator tier.
 */
@Mixin(value = QuantumPatternPredicates.class, remap = false)
public abstract class QuantumPatternPredicatesMixin {

    private static final ResourceLocation CHAOSWORLD$AE2_QUARTZ_VIBRANT_GLASS =
            ResourceLocation.fromNamespaceAndPath("ae2", "quartz_vibrant_glass");

    @Overwrite(remap = false)
    public static Map<Character, BlockState> getDefaultCreativeStates() {
        Map<Character, BlockState> map = new HashMap<>();
        map.put('C', MultiblockBlocks.QUANTUM_HYPER_MECHANICAL_CASING.get().defaultBlockState());
        map.put('F', MultiblockBlocks.STELLAR_FIELD_GENERATOR_T1.get().defaultBlockState());
        map.put('P', MultiblockBlocks.QUANTUM_PATTERN_HATCH.get().defaultBlockState());

        Block vibrantGlass = BuiltInRegistries.BLOCK.get(CHAOSWORLD$AE2_QUARTZ_VIBRANT_GLASS);
        if (vibrantGlass != null && vibrantGlass != Blocks.AIR) {
            map.put('G', vibrantGlass.defaultBlockState());
        }

        return map;
    }

    @Overwrite(remap = false)
    public static Component casingName() {
        return MultiblockBlocks.QUANTUM_HYPER_MECHANICAL_CASING.get().getName();
    }

    @Overwrite(remap = false)
    public static Component casingOrHatchName() {
        return Component.translatable("message.ufo.multiblock.casing_or_hatch");
    }

    @Overwrite(remap = false)
    public static Component patternHatchName() {
        return MultiblockBlocks.QUANTUM_PATTERN_HATCH.get().getName();
    }

    @Overwrite(remap = false)
    public static Component fieldName() {
        return Component.translatable("message.ufo.multiblock.field_generator");
    }

    @Overwrite(remap = false)
    public static List<BlockState> fieldCandidates() {
        return List.of(
                MultiblockBlocks.STELLAR_FIELD_GENERATOR_T1.get().defaultBlockState(),
                MultiblockBlocks.STELLAR_FIELD_GENERATOR_T2.get().defaultBlockState(),
                MultiblockBlocks.STELLAR_FIELD_GENERATOR_T3.get().defaultBlockState()
        );
    }

    @Overwrite(remap = false)
    public static Component glassName() {
        Block block = BuiltInRegistries.BLOCK.get(CHAOSWORLD$AE2_QUARTZ_VIBRANT_GLASS);
        return block != null && block != Blocks.AIR
                ? block.getName()
                : Component.literal("AE2 Quartz Vibrant Glass");
    }
}
