package com.yongaishide.chaosworld.mixin.twilightforest;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import twilightforest.config.TFConfig;
import twilightforest.entity.boss.IBossLootBuffer;

import java.util.List;

/**
 * Replaces Twilight Forest's hardcoded 27-slot boss loot logic with a 243-slot version, matching
 * the Avaritia compressed chest that replaces the reward chest. This removes the 27-item cap and
 * the "drop everything above 27 on the ground" behaviour: all generated loot (including looting
 * bonuses) is stored in the boss's loot buffer and deposited into the chest, sequentially.
 */
@Mixin(value = IBossLootBuffer.class, remap = false)
public interface IBossLootBufferMixin {

    /**
     * Randomly shuffled slots 0..242, matching Twilight Forest's original behaviour (which
     * shuffled 0..26) but covering the full 243-slot compressed chest.
     */
    @Overwrite(remap = false)
    default List<Integer> getAvailableSlots(RandomSource random) {
        ObjectArrayList<Integer> slots = new ObjectArrayList<>(243);
        for (int i = 0; i < 243; i++) {
            slots.add(i);
        }
        Util.shuffle(slots, random);
        return slots;
    }

    /**
     * Stores all generated loot (up to 243 stacks) into the boss's loot buffer at random slot
     * positions. No ground drops.
     */
    @Overwrite(remap = false)
    static <T extends LivingEntity & IBossLootBuffer> void saveDropsIntoBoss(T boss, LootParams params, ServerLevel level) {
        if (!TFConfig.bossDropChests) {
            return;
        }
        LootTable table = level.getServer().reloadableRegistries().getLootTable(boss.getLootTable());
        RandomSource random = boss.getRandom();
        ObjectArrayList<ItemStack> items = table.getRandomItems(params);
        List<Integer> slots = boss.getAvailableSlots(random);
        NonNullList<ItemStack> buffer = boss.getItemStacks();
        for (ItemStack stack : items) {
            if (slots.isEmpty()) {
                break;
            }
            int slot = slots.removeLast();
            buffer.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
    }

    /**
     * Places the reward chest and deposits all loot; tries positions above if the spot is blocked.
     */
    @Overwrite(remap = false)
    static <T extends LivingEntity & IBossLootBuffer> void depositDropsIntoChest(T boss, BlockState blockState, BlockPos pos, ServerLevel level) {
        if (!TFConfig.bossDropChests || boss.getItemStacks().isEmpty()) {
            return;
        }
        if (tryDeposit(boss, blockState, pos, level)) {
            return;
        }
        BlockPos.MutableBlockPos mutable = pos.mutable();
        int y = pos.getY();
        while (y < level.getMaxBuildHeight()) {
            mutable.setY(y);
            if (tryDeposit(boss, blockState, mutable, level)) {
                return;
            }
            y++;
        }
        for (int i = 0; i < 243; i++) {
            ItemStack stack = boss.getItem(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
            }
        }
        IBossLootBuffer.celebrateAt(boss, pos.getCenter(), level);
    }

    /**
     * Places the block if possible and deposits up to 243 stacks into the container.
     */
    @Overwrite(remap = false)
    static <T extends LivingEntity & IBossLootBuffer> boolean tryDeposit(T boss, BlockState blockState, BlockPos pos, ServerLevel level) {
        if (!level.getBlockState(pos).is(blockState.getBlock())) {
            if (level.getBlockState(pos).canBeReplaced() || level.getBlockState(pos).getPistonPushReaction() == PushReaction.BLOCK) {
                if (level.getBlockEntity(pos) != null) {
                    return false;
                }
                if (!level.setBlock(pos, blockState, 2)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container container) {
            int size = Math.min(container.getContainerSize(), 243);
            for (int i = 0; i < size; i++) {
                container.setItem(i, boss.getItem(i));
            }
            IBossLootBuffer.celebrateAt(boss, pos.getCenter(), level);
            return true;
        }
        return false;
    }
}
