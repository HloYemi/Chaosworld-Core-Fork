package com.yongaishide.chaosworld.mekanism.vein.client.gui;

import com.yongaishide.chaosworld.mekanism.vein.tile.TileEntityVeinDrill;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInsetElement;
import mekanism.common.MekanismLang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 虚脉钻探机新增的"钻头/矿脉"升级 tab(与标准 GuiUpgradeWindowTab 同款外观),
 * 点击打开模仿 Mekanism GuiUpgradeWindow 布局的独立升级窗口。
 */
public class VeinUpgradeTab extends GuiInsetElement<TileEntityVeinDrill> {

    public static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("mekanism", "gui/upgrade.png");

    public VeinUpgradeTab(IGuiWrapper gui, TileEntityVeinDrill tile) {
        //与原版 GuiUpgradeWindowTab 同尺寸:宽 26、内图 18(参数序:x, y, height, innerSize)
        super(ICON, gui, tile, gui.getXSize(), 68, 26, 18, false);
        setTooltip(MekanismLang.UPGRADES);
    }

    @Override
    protected void colorTab(GuiGraphics graphics) {
        mekanism.client.render.MekanismRenderer.color(graphics, mekanism.client.SpecialColors.TAB_UPGRADE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clicked(mouseX, mouseY) && active) {
            int x = (gui().getXSize() - 198) / 2;
            VeinUpgradeWindow window = new VeinUpgradeWindow(gui(), x, 15, dataSource);
            //窗口打开时禁用本 tab,防止重复打开多个(关闭后恢复)
            window.setTabListeners(w -> active = true, w -> active = false);
            active = false;
            gui().addWindow(window);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
