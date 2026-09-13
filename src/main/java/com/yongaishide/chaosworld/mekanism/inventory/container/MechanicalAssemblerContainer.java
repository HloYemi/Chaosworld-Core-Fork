package com.yongaishide.chaosworld.mekanism.inventory.container;

import com.yongaishide.chaosworld.mekanism.tile.TileEntityMechanicalAssembler;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import net.minecraft.world.entity.player.Inventory;

/**
 * 机械组装机容器:覆写玩家物品栏的固定 Y 偏移(MEK 默认 84),
 * 使其与加高后的 GUI 内容区对齐,避免物品栏浮在窗口中间。
 */
public class MechanicalAssemblerContainer extends MekanismTileContainer<TileEntityMechanicalAssembler> {

    public static final int INVENTORY_Y_OFFSET = 88;

    public MechanicalAssemblerContainer(ContainerTypeRegistryObject<?> type, int id, Inventory inv,
          TileEntityMechanicalAssembler tile) {
        super(type, id, inv, tile);
    }

    @Override
    protected int getInventoryYOffset() {
        return INVENTORY_Y_OFFSET;
    }
}
