package com.yongaishide.chaosworld.mekanism.vein.client.gui;

import java.util.List;
import com.yongaishide.chaosworld.mekanism.vein.tile.TileEntityVeinDrill;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import net.minecraft.network.chat.Component;

/**
 * 虚脉钻探机专属升级窗口:
 * 信息屏靠左;右侧为升级说明(64 个科技锭放入主界面升级槽 -> 等级+1;9 阶锭满64 -> 无限钻头)。
 */
public class VeinUpgradeWindow extends GuiWindow {

    private final TileEntityVeinDrill tile;

    public VeinUpgradeWindow(IGuiWrapper gui, int x, int y, TileEntityVeinDrill tile) {
        super(gui, x, y, 198, 66, SelectedWindowData.UNSPECIFIED);
        this.tile = tile;
        interactionStrategy = InteractionStrategy.ALL;

        //信息屏移到最左:避开顶部栏(关闭按钮 y=6 起,内容从 30 起)
        addChild(new GuiInnerScreen(gui, x + 6, y + 30, 70, 30, this::infoScreenLines));

        //右侧说明区
        addChild(new GuiInnerScreen(gui, x + 90, y + 30, 102, 30, () -> List.of(
              Component.translatable("info.chaosworld_core.vein.upgrade.hint1"),
              Component.translatable("info.chaosworld_core.vein.upgrade.hint2"),
              Component.translatable("info.chaosworld_core.vein.upgrade.hint3"))));
    }

    private List<Component> infoScreenLines() {
        return List.of(
              Component.translatable("info.chaosworld_core.vein.upgrade.drill",
                    tile.isInfiniteDrill() ? Component.translatable("info.chaosworld_core.vein.upgrade.infinite") : tile.getDrillLevel()),
        Component.translatable("info.chaosworld_core.vein.upgrade.drill_output",
              com.yongaishide.chaosworld.mekanism.vein.tile.TileEntityVeinDrill.pow4Public(Math.max(0, tile.getDrillLevel() - 1))));
    }
}
