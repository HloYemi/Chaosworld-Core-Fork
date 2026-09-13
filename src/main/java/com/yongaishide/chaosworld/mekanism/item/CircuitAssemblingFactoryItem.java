package com.yongaishide.chaosworld.mekanism.item;

import com.jerry.mekextras.api.tier.AdvancedTier;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import mekanism.api.text.TextComponentUtil;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.item.block.ItemBlockTooltip;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 电路组装工厂物品:物品名按等级着色,与 mekmm/mekextras 的工厂物品一致
 * (MEK 4 级用 BaseTier 颜色,mekextras 4 级用 AdvancedTier 颜色)。
 */
public class CircuitAssemblingFactoryItem<BLOCK extends Block & mekanism.common.block.interfaces.IHasDescription>
      extends ItemBlockTooltip<BLOCK> {

    public CircuitAssemblingFactoryItem(BLOCK block, boolean hasDetails, Item.Properties properties) {
        super(block, hasDetails, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Holder<Block> holder = getBlock().builtInRegistryHolder();
        BaseTier baseTier = Attribute.getBaseTier(holder);
        if (baseTier != null) {
            return TextComponentUtil.build(baseTier.getColor(), super.getName(stack));
        }
        AdvancedTier advancedTier = ExtraAttribute.getAdvancedTier(holder);
        if (advancedTier != null) {
            return TextComponentUtil.build(advancedTier.getColor(), super.getName(stack));
        }
        return super.getName(stack);
    }
}
