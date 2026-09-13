package com.yongaishide.chaosworld.mekanism.vein;

import java.util.List;
import com.yongaishide.chaosworld.mekanism.vein.VeinData.VeinNbt;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 虚拟矿脉核心:携带矿脉数据(大小等级/质量剩余)的物品。
 * 放入虚脉钻探机的核心槽后,机器显示并绑定该矿脉信息;手持可查看矿脉详情。
 */
public class VeinCoreItem extends Item {

    public VeinCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        VeinData data = VeinNbt.read(stack);
        if (data == null) {
            tooltip.add(Component.translatable("item.chaosworld_core.vein_core.tooltip.empty"));
            return;
        }
        tooltip.add(Component.translatable("info.chaosworld_core.vein.size",
              Component.translatable(data.sizeName()))
              .withStyle(Style.EMPTY.withColor(0xFFB0B0)));
        tooltip.add(Component.translatable("info.chaosworld_core.vein.mass",
              formatMass(data.massRemaining()), formatMass(data.massCapacity())));
    }

    private static String formatMass(long mass) {
        if (mass >= 1_000_000) {
            return String.format("%.1fM", mass / 1_000_000.0);
        } else if (mass >= 1_000) {
            return String.format("%.1fK", mass / 1_000.0);
        }
        return String.valueOf(mass);
    }
}
