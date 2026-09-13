package com.yongaishide.chaosworld.mekanism.client.gui;

import com.yongaishide.chaosworld.mekanism.client.recipe_viewer.DragonSoulForgingRVRecipeType;
import com.yongaishide.chaosworld.mekanism.inventory.container.DragonSoulForgeContainer;
import com.yongaishide.chaosworld.mekanism.tile.TileEntityDragonSoulForge;
import com.yongaishide.chaosworld.mekanism.tile.TileEntityMechanicalAssembler;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.bar.GuiFluidBar;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * 龙魂锻炉 GUI:单机为 176 宽标准布局(流体槽 + 3x3 网格 + 进度/输出/催化剂/能量);
 * 工厂版为 mekmm 高级工厂式列式布局——上排 N 格物品输入、箭头行、第二排[催化剂 + N 格输出],
 * 流体槽在催化剂格正上方,窗口随线程数加宽。
 */
public class GuiDragonSoulForge extends GuiConfigurableTile<TileEntityMechanicalAssembler,
      DragonSoulForgeContainer> {

    public GuiDragonSoulForge(DragonSoulForgeContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
        //窗口高度须覆盖容器覆写的玩家物品栏偏移 + 物品栏高度
        imageHeight = container.getEffectiveInventoryYOffset() + 83 + 5;
        inventoryLabelY = container.getEffectiveInventoryYOffset() - 12;
        imageWidth = 176;
    }

    @Override
    protected void addGuiElements() {
        //注意:安全/升级等侧边 tab 在 super.addGuiElements() 中按当时的 GUI 宽度创建,
        //因此必须在 super 之前先把工厂宽窗口设好,否则 tab 会停在旧宽度(视觉居中)
        TileEntityDragonSoulForge forge = tile instanceof TileEntityDragonSoulForge f && f.isFactoryLayout() ? f : null;
        if (forge != null) {
            imageWidth = forge.getFactoryGuiWidth();
        }
        super.addGuiElements();
        addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), tile::getActive));
        if (forge != null) {
            addFactoryElements(forge);
        } else {
            //单机:流体槽(17,15)+ 进度条 + 能量条
            addRenderableWidget(new GuiFluidGauge(() -> tile.inputFluidTank1, () -> tile.getFluidTanks(null), GaugeType.STANDARD, this, 17, 15))
                  .warning(WarningType.NO_MATCHING_RECIPE, tile.getWarningCheck(RecipeError.NOT_ENOUGH_INPUT));
            addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 161, 23)
                  .warning(WarningType.NOT_ENOUGH_ENERGY, tile.getWarningCheck(RecipeError.NOT_ENOUGH_ENERGY)));
            addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.TALL_RIGHT, this, 97, 34)
                  .recipeViewerCategories(DragonSoulForgingRVRecipeType.INSTANCE))
                  .warning(WarningType.INPUT_DOESNT_PRODUCE_OUTPUT, tile.getWarningCheck(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT));
            return;
        }
    }

    /**
     * 工厂列式布局:18x18 小流体槽(催化格正上方)+ 每线程向下的进度箭头 + 能量条(右列)。
     * 行位置与窗口宽由 tile 统一计算(内容自动居中),槽位由 tile 布置,GUI 自动绘制。
     */
    private void addFactoryElements(TileEntityDragonSoulForge forge) {
        int rowX = forge.getFactoryRowX();
        //18x18 流体格子(同 mekmm 高级工厂的 GuiFluidBar),催化格上方(左移 1px、上移 3px)
        addRenderableWidget(new GuiFluidBar(this,
              GuiFluidBar.getProvider(tile.inputFluidTank1, tile.getFluidTanks(null)), rowX - 1, 41, 18, 18, true))
              .warning(WarningType.NO_MATCHING_RECIPE, tile.getWarningCheck(RecipeError.NOT_ENOUGH_INPUT));
        for (int i = 0; i < forge.getThreadCount(); i++) {
            addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.DOWN, this, rowX + 18 + i * 18 + 3, 40)
                  .recipeViewerCategories(DragonSoulForgingRVRecipeType.INSTANCE))
                  .warning(WarningType.INPUT_DOESNT_PRODUCE_OUTPUT, tile.getWarningCheck(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT));
        }
        //能量条:右列竖条(左移 18px、下移 8px)
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), forge.getFactoryGuiWidth() - 34, 19)
              .warning(WarningType.NOT_ENOUGH_ENERGY, tile.getWarningCheck(RecipeError.NOT_ENOUGH_ENERGY)));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
