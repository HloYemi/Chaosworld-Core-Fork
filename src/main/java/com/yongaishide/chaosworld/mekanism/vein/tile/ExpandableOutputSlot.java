package com.yongaishide.chaosworld.mekanism.vein.tile;

import java.util.function.IntSupplier;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 可扩容输出槽:单格堆叠上限 = baseLimit × 2^(等级-1)(等级1 = 64)。
 * 等级由 IntSupplier 实时提供,升级后立即生效。
 */
public class ExpandableOutputSlot extends BasicInventorySlot {

    private final int baseLimit;
    private final IntSupplier levelSupplier;

    protected ExpandableOutputSlot(int baseLimit, IntSupplier levelSupplier, @Nullable IContentsListener listener, int x, int y) {
        super(Item.ABSOLUTE_MAX_STACK_SIZE,
              (stack, automationType) -> automationType == AutomationType.MANUAL || automationType == AutomationType.INTERNAL,
              (stack, automationType) -> true,
              stack -> true, listener, x, y);
        this.baseLimit = baseLimit;
        this.levelSupplier = levelSupplier;
        this.obeyStackLimit = false;
        setSlotType(ContainerSlotType.OUTPUT);
    }

    public static ExpandableOutputSlot at(int baseLimit, IntSupplier levelSupplier, @Nullable IContentsListener listener, int x, int y) {
        return new ExpandableOutputSlot(baseLimit, levelSupplier, listener, x, y);
    }

    @Override
    public int getLimit(ItemStack stack) {
        int n = Math.max(0, levelSupplier.getAsInt() - 1);
        return baseLimit * com.yongaishide.chaosworld.mekanism.vein.tile.TileEntityVeinDrill.pow4Public(n);
    }
}
