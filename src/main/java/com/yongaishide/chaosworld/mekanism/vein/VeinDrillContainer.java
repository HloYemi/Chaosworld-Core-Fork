package com.yongaishide.chaosworld.mekanism.vein;

import com.yongaishide.chaosworld.mekanism.vein.tile.TileEntityVeinDrill;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.world.entity.player.Inventory;

/**
 * 虚脉钻探机容器:将玩家物品栏下移到输出网格下方。
 */
public class VeinDrillContainer extends MekanismTileContainer<TileEntityVeinDrill> {

    /** 玩家物品栏 Y 偏移(输出气体行底 204 + 2 间距) */
    public static final int INVENTORY_Y_OFFSET = 206;

    private InventoryContainerSlot upgradeContainerSlot;

    public VeinDrillContainer(int id, Inventory playerInv, TileEntityVeinDrill tile) {
        super(VeinDrillMachines.VEIN_DRILL_CONTAINER, id, playerInv, tile);
    }

    public void setUpgradeContainerSlot(InventoryContainerSlot slot) {
        this.upgradeContainerSlot = slot;
    }

    public InventoryContainerSlot getUpgradeContainerSlot() {
        return upgradeContainerSlot;
    }

    @Override
    protected int getInventoryYOffset() {
        return INVENTORY_Y_OFFSET;
    }
}
