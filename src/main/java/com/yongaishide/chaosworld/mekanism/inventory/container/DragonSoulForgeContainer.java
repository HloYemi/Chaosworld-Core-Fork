package com.yongaishide.chaosworld.mekanism.inventory.container;

import com.yongaishide.chaosworld.mekanism.tile.TileEntityMechanicalAssembler;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import net.minecraft.world.entity.player.Inventory;

/**
 * 龙魂锻炉容器:布局与电路组装机完全一致(暂定),故直接复用其槽位排布,
 * 仅作为独立的 MenuType 注册,以便锻炉拥有单独的 GUI 入口。
 */
public class DragonSoulForgeContainer extends MechanicalAssemblerContainer {

    public DragonSoulForgeContainer(ContainerTypeRegistryObject<?> type, int id, Inventory inv,
          TileEntityMechanicalAssembler tile) {
        super(type, id, inv, tile);
    }

    /**
     * 工厂版列式布局:玩家物品栏与窗口背景中心对齐(同 mekmm 高级工厂);
     * 单机保持默认左侧对齐(176 宽)。
     */
    @Override
    protected int getInventoryXOffset() {
        if (isFactoryLayout()) {
            return Math.max(8, (((com.yongaishide.chaosworld.mekanism.tile.TileEntityDragonSoulForge) tile)
                  .getFactoryGuiWidth() - 9 * 18) / 2 - 2);
        }
        return super.getInventoryXOffset();
    }

    /**
     * 工厂版内容整体下移 10px(避开标题),玩家物品栏相应下移。
     */
    @Override
    protected int getInventoryYOffset() {
        return isFactoryLayout() ? INVENTORY_Y_OFFSET + 10 : super.getInventoryYOffset();
    }

    /** 供 GUI 读取实际物品栏 Y 偏移(工厂 98 / 单机 88) */
    public int getEffectiveInventoryYOffset() {
        return getInventoryYOffset();
    }

    private boolean isFactoryLayout() {
        return tile instanceof com.yongaishide.chaosworld.mekanism.tile.TileEntityDragonSoulForge forge
              && forge.isFactoryLayout();
    }
}
