package com.yongaishide.chaosworld.mekanism.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * JSON 格式:
 * <pre>
 * {
 *   "type": "chaosworld_core:assembling",
 *   "itemInputs": [
 *     {"item": "minecraft:iron_ingot"},
 *     {"tag": "c:ingots/steel"}
 *   ],
 *   "chemicalInput": {"gas": "mekanism:hydrogen", "amount": 100},
 *   "fluidInputs": [
 *     {"fluid": "minecraft:water", "amount": 50}
 *   ],
 *   "output": {"item": "minecraft:diamond"}
 * }
 * </pre>
 * fluidInputs 可省略或留空数组(0~2 种,按槽位顺序匹配)。
 */
public record AssemblingRecipeSerializer(MapCodec<BasicAssemblingRecipe> codec,
                                         StreamCodec<RegistryFriendlyByteBuf, BasicAssemblingRecipe> streamCodec)
      implements RecipeSerializer<BasicAssemblingRecipe> {

    public static final AssemblingRecipeSerializer INSTANCE = new AssemblingRecipeSerializer(
          RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemInput.CODEC.listOf().fieldOf("itemInputs").forGetter(BasicAssemblingRecipe::getItemInputsWithCount),
                ChemicalStackIngredient.CODEC.fieldOf("chemicalInput").forGetter(BasicAssemblingRecipe::getChemicalInput),
                FluidStackIngredient.CODEC.listOf().optionalFieldOf("fluidInputs", List.of()).forGetter(BasicAssemblingRecipe::getFluidInputs),
                ItemStack.CODEC.fieldOf("output").forGetter(BasicAssemblingRecipe::getOutputRaw)
          ).apply(instance, BasicAssemblingRecipe::new)),
          StreamCodec.composite(
                ItemInput.STREAM_CODEC.apply(ByteBufCodecs.list()), BasicAssemblingRecipe::getItemInputsWithCount,
                ChemicalStackIngredient.STREAM_CODEC, BasicAssemblingRecipe::getChemicalInput,
                FluidStackIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), BasicAssemblingRecipe::getFluidInputs,
                ItemStack.STREAM_CODEC, BasicAssemblingRecipe::getOutputRaw,
                BasicAssemblingRecipe::new
          )
    );
}
