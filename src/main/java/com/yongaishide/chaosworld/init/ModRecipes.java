package com.yongaishide.chaosworld.init;

import com.yongaishide.chaosworld.recipe.FusionConversionRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, "chaosworld_core");

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, "chaosworld_core");

    // FUSION CONVERSION (data-driven fusion reactor plasma conversion)
    public static final String FUSION_CONVERSION_ID = "fusion_conversion";

    public static final Supplier<RecipeType<FusionConversionRecipe>> FUSION_CONVERSION_TYPE = RECIPE_TYPES.register(FUSION_CONVERSION_ID, () -> new RecipeType<FusionConversionRecipe>() {
        @Override
        public String toString() {
            return FUSION_CONVERSION_ID;
        }
    });

    public static final Supplier<RecipeSerializer<FusionConversionRecipe>> FUSION_CONVERSION_SERIALIZER =
            SERIALIZERS.register(FUSION_CONVERSION_ID, FusionConversionRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        RECIPE_TYPES.register(eventBus);
    }
}
