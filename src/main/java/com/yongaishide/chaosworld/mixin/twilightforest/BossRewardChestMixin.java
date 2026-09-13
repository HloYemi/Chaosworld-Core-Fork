package com.yongaishide.chaosworld.mixin.twilightforest;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.boss.BaseTFBoss;

/**
 * Swaps the normal boss reward chest that Twilight Forest spawns after a boss is defeated for
 * Avaritia's {@code compressed_chest}, and enlarges the boss loot buffer to 243 slots so every
 * generated item (including looting bonuses) ends up inside the chest.
 *
 * <p>The 27-slot hardcoding in {@code IBossLootBuffer} is replaced by
 * {@code IBossLootBufferMixin}; this mixin only swaps the container block and provides the larger
 * loot buffer backing {@link BaseTFBoss#getItemStacks()}.</p>
 */
@Mixin(value = BaseTFBoss.class, remap = false)
public class BossRewardChestMixin {

    @Unique
    private final NonNullList<ItemStack> chaosworld$lootItems = NonNullList.withSize(243, ItemStack.EMPTY);

    @Overwrite(remap = false)
    public NonNullList<ItemStack> getItemStacks() {
        return chaosworld$lootItems;
    }

    @Redirect(
            method = "postRemoval(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity$RemovalReason;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ltwilightforest/entity/boss/BaseTFBoss;getDeathContainer(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/level/block/Block;"
            ),
            remap = false
    )
    private static Block chaosworld$swapBossRewardChest(BaseTFBoss boss, RandomSource random) {
        Block chest = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("avaritia:compressed_chest"));
        return chest != null ? chest : boss.getDeathContainer(random);
    }
}
