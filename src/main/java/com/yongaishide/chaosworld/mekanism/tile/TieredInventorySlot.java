package com.yongaishide.chaosworld.mekanism.tile;

import java.util.function.IntSupplier;
import java.util.function.Predicate;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.world.item.ItemStack;

/**
 * 可扩容堆叠的物品槽:仿 mekextras 的 {@code StackableInputInventorySlot} 实现——
 * {@code getLimit = super.getLimit(stack) × (1 << shift)},每级堆叠翻倍。
 * shift 由工厂等级(每级 +1)与堆叠升级(每级 +1)共同驱动,运行时动态生效。
 */
public class TieredInventorySlot extends BasicInventorySlot {

    private final IntSupplier shiftSupplier;

    public TieredInventorySlot(Predicate<ItemStack> validator, IContentsListener listener, int x, int y,
          IntSupplier shiftSupplier) {
        super(validator, validator, validator, listener, x, y);
        this.shiftSupplier = shiftSupplier;
    }

    @Override
    public int getLimit(ItemStack stack) {
        try {
            int shift = Math.max(0, shiftSupplier.getAsInt());
            if (shift > 30) {
                return Integer.MAX_VALUE;
            }
            return Math.multiplyExact(super.getLimit(stack), 1 << shift);
        } catch (ArithmeticException e) {
            return Integer.MAX_VALUE;
        }
    }
}
