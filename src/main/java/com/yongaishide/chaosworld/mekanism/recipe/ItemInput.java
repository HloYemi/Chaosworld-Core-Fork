package com.yongaishide.chaosworld.mekanism.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 批量物品输入:每种配方材料一次消耗 {@link #count} 个。
 * <p>
 * JSON 兼容两种形式:
 * <pre>
 * {"ingredient": {"item": "minecraft:diamond"}, "count": 2}   // 新格式(批量)
 * {"item": "minecraft:coal"}                                   // 旧格式(count=1)
 * </pre>
 */
@NothingNullByDefault
public record ItemInput(ItemStackIngredient ingredient, int count) {

    public static final MapCodec<ItemInput> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          ItemStackIngredient.CODEC.fieldOf("ingredient").forGetter(ItemInput::ingredient),
          Codec.INT.optionalFieldOf("count", 1).forGetter(ItemInput::count)
    ).apply(instance, ItemInput::new));

    /** 兼容旧格式(无 count)的解码 */
    public static final Codec<ItemInput> CODEC = Codec.either(MAP_CODEC.codec(), ItemStackIngredient.CODEC)
          .xmap(either -> either.map(itemInput -> itemInput, ingredient -> new ItemInput(ingredient, 1)),
                itemInput -> Either.left(itemInput));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemInput> STREAM_CODEC = StreamCodec.composite(
          ItemStackIngredient.STREAM_CODEC, ItemInput::ingredient,
          ByteBufCodecs.VAR_INT, ItemInput::count,
          ItemInput::new);

    public ItemInput {
        if (ingredient == null) {
            throw new IllegalArgumentException("Ingredient cannot be null.");
        }
        if (count < 1) {
            throw new IllegalArgumentException("Item input count must be at least 1.");
        }
    }
}
