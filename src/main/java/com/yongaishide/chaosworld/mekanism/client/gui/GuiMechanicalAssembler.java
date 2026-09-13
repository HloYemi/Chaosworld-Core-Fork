package com.yongaishide.chaosworld.mekanism.client.gui;

import com.yongaishide.chaosworld.mekanism.client.recipe_viewer.AssemblingRVRecipeType;
import com.yongaishide.chaosworld.mekanism.inventory.container.MechanicalAssemblerContainer;
import com.yongaishide.chaosworld.mekanism.tile.TileEntityMechanicalAssembler;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
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
 * 机械组装机 GUI(220 宽):
 * 左侧气体/流体计量表+填充槽横排 3 列,中部 3x3 网格,
 * 右侧进度条对齐玩家物品栏第二行,输出槽在进度条右面,能量条最右,能量槽在能量条上方。
 */
public class GuiMechanicalAssembler extends GuiConfigurableTile<TileEntityMechanicalAssembler,
      MechanicalAssemblerContainer> {

    public GuiMechanicalAssembler(MechanicalAssemblerContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
        //窗口宽度 = 能量条右缘(181)+ 右边距,避免右侧空出大片背景
        imageWidth = 190;
        //窗口高度须覆盖容器覆写的玩家物品栏偏移(156) + 物品栏高度
        imageHeight = MechanicalAssemblerContainer.INVENTORY_Y_OFFSET + 83 + 5;
        inventoryLabelY = MechanicalAssemblerContainer.INVENTORY_Y_OFFSET - 12;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), tile::getActive));
        addRenderableWidget(new GuiChemicalGauge(() -> tile.inputTank, () -> tile.getChemicalTanks(null), GaugeType.STANDARD, this, 5, 15))
              .warning(WarningType.NO_MATCHING_RECIPE, tile.getWarningCheck(RecipeError.NOT_ENOUGH_INPUT));
        addRenderableWidget(new GuiFluidGauge(() -> tile.inputFluidTank1, () -> tile.getFluidTanks(null), GaugeType.STANDARD, this, 27, 15))
              .warning(WarningType.NO_MATCHING_RECIPE, tile.getWarningCheck(RecipeError.NOT_ENOUGH_INPUT));
        addRenderableWidget(new GuiFluidGauge(() -> tile.inputFluidTank2, () -> tile.getFluidTanks(null), GaugeType.STANDARD, this, 49, 15))
              .warning(WarningType.NO_MATCHING_RECIPE, tile.getWarningCheck(RecipeError.NOT_ENOUGH_INPUT));
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 175, 23)
              .warning(WarningType.NOT_ENOUGH_ENERGY, tile.getWarningCheck(RecipeError.NOT_ENOUGH_ENERGY)));
        addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.TALL_RIGHT, this, 129, 34)
              .recipeViewerCategories(AssemblingRVRecipeType.INSTANCE))
              .warning(WarningType.INPUT_DOESNT_PRODUCE_OUTPUT, tile.getWarningCheck(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
