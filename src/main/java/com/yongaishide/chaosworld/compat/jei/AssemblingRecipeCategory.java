package com.yongaishide.chaosworld.compat.jei;

import com.mojang.serialization.Codec;
import com.yongaishide.chaosworld.mekanism.client.recipe_viewer.AssemblingRVRecipeType;
import com.yongaishide.chaosworld.mekanism.recipe.BasicAssemblingRecipe;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.recipe_viewer.jei.BaseRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;

/**
 * 机械组装机 JEI 配方类别:布局与机器 GUI 一致——
 * 3x3 物品网格 + 化学物/流体计量表 + 进度条 + 输出槽,全部使用 MEK 的 GUI 元素渲染。
 */
public class AssemblingRecipeCategory extends BaseRecipeCategory<BasicAssemblingRecipe> {

    public static final RecipeType<BasicAssemblingRecipe> RECIPE_TYPE =
            RecipeType.create("chaosworld_core", "assembling", BasicAssemblingRecipe.class);

    private final GuiSlot[] gridSlots = new GuiSlot[9];
    private final GuiGauge<?> chemicalGauge;
    private final GuiGauge<?> fluidGauge1;
    private final GuiGauge<?> fluidGauge2;
    private final GuiSlot outputSlot;

    public AssemblingRecipeCategory(IJeiHelpers helpers) {
        super(helpers.getGuiHelper(), AssemblingRVRecipeType.INSTANCE);
        //3x3 物品输入网格(与机器 GUI 坐标一致)
        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            gridSlots[i] = addSlot(SlotType.INPUT, 73 + col * 18, 16 + row * 18);
        }
        //化学物/流体计量表(横排,与机器 GUI 一致)
        chemicalGauge = addElement(GuiChemicalGauge.getDummy(GaugeType.STANDARD, this, 5, 15));
        fluidGauge1 = addElement(GuiFluidGauge.getDummy(GaugeType.STANDARD, this, 27, 15));
        fluidGauge2 = addElement(GuiFluidGauge.getDummy(GaugeType.STANDARD, this, 49, 15));
        //输出槽 + 进度条
        outputSlot = addSlot(SlotType.OUTPUT, 153, 34);
        addSimpleProgress(ProgressType.TALL_RIGHT, 131, 34);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BasicAssemblingRecipe recipe, IFocusGroup focuses) {
        //3x3 物品输入(无序匹配,按批量消耗数量显示)
        java.util.List<com.yongaishide.chaosworld.mekanism.recipe.ItemInput> itemInputs = recipe.getItemInputsWithCount();
        for (int i = 0; i < itemInputs.size() && i < 9; i++) {
            final com.yongaishide.chaosworld.mekanism.recipe.ItemInput input = itemInputs.get(i);
            initItem(builder, RecipeIngredientRole.INPUT, gridSlots[i],
                  input.ingredient().getRepresentations().stream()
                        .map(stack -> stack.copyWithCount(input.count()))
                        .toList());
        }
        //化学物输入
        initChemical(builder, RecipeIngredientRole.INPUT, chemicalGauge, recipe.getChemicalInput().getRepresentations())
              .setSlotName("chemicalInput");
        //流体输入(按槽位顺序)
        java.util.List<FluidStackIngredient> fluidInputs = recipe.getFluidInputs();
        if (!fluidInputs.isEmpty()) {
            initFluid(builder, RecipeIngredientRole.INPUT, fluidGauge1, fluidInputs.get(0).getRepresentations())
                  .setSlotName("fluidInput1");
        }
        if (fluidInputs.size() > 1) {
            initFluid(builder, RecipeIngredientRole.INPUT, fluidGauge2, fluidInputs.get(1).getRepresentations())
                  .setSlotName("fluidInput2");
        }
        //输出
        initItem(builder, RecipeIngredientRole.OUTPUT, outputSlot, recipe.getOutputDefinition());
    }

    @Override
    public ResourceLocation getRegistryName(BasicAssemblingRecipe recipe) {
        return null;
    }

    @Override
    public Codec<BasicAssemblingRecipe> getCodec(mezz.jei.api.helpers.ICodecHelper helper,
          mezz.jei.api.recipe.IRecipeManager manager) {
        return null;
    }
}
