package com.yongaishide.chaosworld.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yongaishide.chaosworld.init.ModRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Data-driven conversion performed when an item is dropped into the plasma of a
 * burning Mekanism fusion reactor.
 *
 * <pre>
 * {
 *   "type": "chaosworld_core:fusion_conversion",
 *   "input": {"item": "chaosworld_core:zeluosishuijing"},
 *   "result": {"id": "chaosworld_core:neutronite_ingot", "count": 1}
 * }
 * </pre>
 *
 * Additional conversions can be added by any data pack (e.g. KubeJS).
 */
public class FusionConversionRecipe implements Recipe<RecipeInput> {

    private final Ingredient input;
    private final ItemStack result;

    public FusionConversionRecipe(Ingredient input, ItemStack result) {
        this.input = input;
        this.result = result;
    }

    public Ingredient getInput() {
        return input;
    }

    public ItemStack getResult() {
        return result;
    }

    @Override
    public boolean matches(@NotNull RecipeInput recipeInput, @NotNull Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput recipeInput, HolderLookup.@NotNull Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return result.copy();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.FUSION_CONVERSION_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.FUSION_CONVERSION_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<FusionConversionRecipe> {
        public static final MapCodec<FusionConversionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("input").forGetter(FusionConversionRecipe::getInput),
                ItemStack.CODEC.fieldOf("result").forGetter(FusionConversionRecipe::getResult)
        ).apply(instance, FusionConversionRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, FusionConversionRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, FusionConversionRecipe::getInput,
                ItemStack.STREAM_CODEC, FusionConversionRecipe::getResult,
                FusionConversionRecipe::new);

        @Override
        public MapCodec<FusionConversionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FusionConversionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
