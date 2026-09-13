package com.yongaishide.chaosworld.mekanism.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
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
 *   "type": "chaosworld_core:dragon_soul_forging",
 *   "itemInputs": [{"item": "minecraft:iron_ingot"}],
 *   "fluidInputs": [{"fluid": "minecraft:water", "amount": 50}],
 *   "catalystInput": {"ingredient": {"item": "chaosworld_core:dragon_catalyst"}, "amount": 1, "consume": true},
 *   "output": {"id": "minecraft:diamond"}
 * }
 * </pre>
 * 锻炉没有化学槽:无化学输入;fluidInputs 上限为 1(唯一流体槽)。
 * catalystInput 可选——硬性门槛(缺少即停摆);consume=true 每份固定消耗,
 * consume=false 仅作门槛不消耗;未声明则无催化剂依赖。
 */
public record DragonSoulForgingRecipeSerializer(MapCodec<BasicDragonSoulForgingRecipe> codec,
                                                StreamCodec<RegistryFriendlyByteBuf, BasicDragonSoulForgingRecipe> streamCodec)
      implements RecipeSerializer<BasicDragonSoulForgingRecipe> {

    public static final DragonSoulForgingRecipeSerializer INSTANCE = new DragonSoulForgingRecipeSerializer(
          RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemInput.CODEC.listOf().fieldOf("itemInputs").forGetter(BasicDragonSoulForgingRecipe::getItemInputsWithCount),
                FluidStackIngredient.CODEC.listOf().optionalFieldOf("fluidInputs", List.of()).forGetter(BasicDragonSoulForgingRecipe::getFluidInputs),
                CatalystInput.CODEC.codec().optionalFieldOf("catalystInput").forGetter(BasicDragonSoulForgingRecipe::getCatalystInput),
                ItemStack.CODEC.fieldOf("output").forGetter(BasicDragonSoulForgingRecipe::getOutputRaw)
          ).apply(instance, (itemInputs, fluidInputs, catalyst, output) ->
                new BasicDragonSoulForgingRecipe(itemInputs, fluidInputs, catalyst.orElse(null), output))),
          StreamCodec.composite(
                ItemInput.STREAM_CODEC.apply(ByteBufCodecs.list()), BasicDragonSoulForgingRecipe::getItemInputsWithCount,
                FluidStackIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), BasicDragonSoulForgingRecipe::getFluidInputs,
                ByteBufCodecs.optional(CatalystInput.STREAM_CODEC), BasicDragonSoulForgingRecipe::getCatalystInput,
                ItemStack.STREAM_CODEC, BasicDragonSoulForgingRecipe::getOutputRaw,
                (itemInputs, fluidInputs, catalyst, output) ->
                      new BasicDragonSoulForgingRecipe(itemInputs, fluidInputs, catalyst.orElse(null), output))
    );
}
