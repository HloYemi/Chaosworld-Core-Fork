package com.yongaishide.chaosworld.mekanism.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 锻炉配方的催化剂输入(额外输入):配方运行必须满足的硬性门槛。
 * <p>
 * {@link #consume()} 决定消耗模式(二选一,不再概率):
 * <ul>
 *   <li>true:每次配方操作完成时<b>固定消耗</b> {@link #amount} 个;</li>
 *   <li>false:催化剂<b>永不消耗</b>,只作为入场资格反复使用。</li>
 * </ul>
 * <p>
 * JSON 示例:
 * <pre>
 * "catalystInput": {"ingredient": {"item": "chaosworld_core:dragon_catalyst"}, "amount": 1, "consume": true}
 * </pre>
 * amount 默认 1;consume 默认 true(消耗型额外输入)。
 */
@NothingNullByDefault
public record CatalystInput(ItemStackIngredient ingredient, int amount, boolean consume) {

    public static final MapCodec<CatalystInput> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          ItemStackIngredient.CODEC.fieldOf("ingredient").forGetter(CatalystInput::ingredient),
          Codec.INT.optionalFieldOf("amount", 1).forGetter(CatalystInput::amount),
          Codec.BOOL.optionalFieldOf("consume", true).forGetter(CatalystInput::consume)
    ).apply(instance, CatalystInput::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CatalystInput> STREAM_CODEC = StreamCodec.composite(
          ItemStackIngredient.STREAM_CODEC, CatalystInput::ingredient,
          ByteBufCodecs.VAR_INT, CatalystInput::amount,
          ByteBufCodecs.BOOL, CatalystInput::consume,
          CatalystInput::new);

    public CatalystInput {
        if (amount < 1) {
            throw new IllegalArgumentException("Catalyst amount must be at least 1.");
        }
    }
}
