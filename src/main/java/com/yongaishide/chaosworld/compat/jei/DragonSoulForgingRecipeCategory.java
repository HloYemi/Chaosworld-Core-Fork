package com.yongaishide.chaosworld.compat.jei;

import com.mojang.serialization.Codec;
import com.yongaishide.chaosworld.mekanism.client.recipe_viewer.DragonSoulForgingRVRecipeType;
import com.yongaishide.chaosworld.mekanism.recipe.BasicDragonSoulForgingRecipe;
import com.yongaishide.chaosworld.mekanism.recipe.CatalystInput;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
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
import net.minecraft.world.item.ItemStack;

/**
 * 龙魂锻炉 JEI 配方类别:布局与锻炉 GUI 一致——
 * 3x3 物品网格(x=37 起)+ 流体计量表 + 进度条 + 输出槽 + 催化剂槽,无化学物部分。
 */
public class DragonSoulForgingRecipeCategory extends BaseRecipeCategory<BasicDragonSoulForgingRecipe> {

    public static final RecipeType<BasicDragonSoulForgingRecipe> RECIPE_TYPE =
          RecipeType.create("chaosworld_core", "dragon_soul_forging", BasicDragonSoulForgingRecipe.class);

    private final GuiSlot[] gridSlots = new GuiSlot[9];
    private final GuiGauge<?> fluidGauge;
    private final GuiSlot catalystSlot;
    private final GuiSlot outputSlot;

    public DragonSoulForgingRecipeCategory(IJeiHelpers helpers) {
        super(helpers.getGuiHelper(), DragonSoulForgingRVRecipeType.INSTANCE);
        //3x3 物品输入网格(与锻炉 GUI 坐标一致,整体左移 32px)
        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            gridSlots[i] = addSlot(SlotType.INPUT, 41 + col * 18, 16 + row * 18);
        }
        //流体计量表(贴网格左侧)
        fluidGauge = addElement(GuiFluidGauge.getDummy(GaugeType.STANDARD, this, 17, 15));
        //催化剂槽(额外输入,进度条正下方)
        catalystSlot = addSlot(SlotType.EXTRA, 99, 59);
        //输出槽 + 进度条(与机器 GUI 坐标一致)
        outputSlot = addSlot(SlotType.OUTPUT, 119, 34);
        addSimpleProgress(ProgressType.TALL_RIGHT, 97, 34);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BasicDragonSoulForgingRecipe recipe, IFocusGroup focuses) {
        //物品输入:按配方的批量消耗数量直接显示(如 2 钻石 -> 一格 ×2)
        java.util.List<com.yongaishide.chaosworld.mekanism.recipe.ItemInput> itemInputs = recipe.getItemInputsWithCount();
        for (int i = 0; i < itemInputs.size() && i < 9; i++) {
            final com.yongaishide.chaosworld.mekanism.recipe.ItemInput input = itemInputs.get(i);
            initItem(builder, RecipeIngredientRole.INPUT, gridSlots[i],
                  input.ingredient().getRepresentations().stream()
                        .map(stack -> stack.copyWithCount(input.count()))
                        .toList());
        }
        //流体输入(按槽位顺序)
        java.util.List<FluidStackIngredient> fluidInputs = recipe.getFluidInputs();
        if (!fluidInputs.isEmpty()) {
            initFluid(builder, RecipeIngredientRole.INPUT, fluidGauge, fluidInputs.get(0).getRepresentations())
                  .setSlotName("fluidInput");
        }
        //催化剂(额外输入):按配方展示消耗模式(消耗/不消耗)
        if (recipe.getCatalystInput().isPresent()) {
            CatalystInput catalyst = recipe.getCatalystInput().get();
            initItem(builder, RecipeIngredientRole.CATALYST, catalystSlot,
                  catalyst.ingredient().getRepresentations().stream()
                        .map(stack -> stack.copyWithCount(catalyst.amount()))
                        .toList())
                  .setSlotName("catalyst")
                  .addTooltipCallback((view, tooltip) -> tooltip.add(
                        net.minecraft.network.chat.Component.translatable(
                              catalyst.consume()
                                    ? "jei.chaosworld_core.dragon_soul_forging.catalyst_consume"
                                    : "jei.chaosworld_core.dragon_soul_forging.catalyst_keep")));
        }
        //输出
        initItem(builder, RecipeIngredientRole.OUTPUT, outputSlot, recipe.getOutputDefinition());
    }

    @Override
    public ResourceLocation getRegistryName(BasicDragonSoulForgingRecipe recipe) {
        return null;
    }

    @Override
    public Codec<BasicDragonSoulForgingRecipe> getCodec(mezz.jei.api.helpers.ICodecHelper helper,
          mezz.jei.api.recipe.IRecipeManager manager) {
        return null;
    }
}
