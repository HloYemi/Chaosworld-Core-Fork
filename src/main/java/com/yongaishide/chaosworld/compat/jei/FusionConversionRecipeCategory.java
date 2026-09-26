package com.yongaishide.chaosworld.compat.jei;

import com.yongaishide.chaosworld.recipe.FusionConversionRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FusionConversionRecipeCategory implements IRecipeCategory<FusionConversionRecipe> {
    public static final RecipeType<FusionConversionRecipe> RECIPE_TYPE =
            RecipeType.create("chaosworld_core", "fusion_conversion", FusionConversionRecipe.class);

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("chaosworld_core", "textures/guis/dimensional_matter_assembler_jei_ui.png");

    private static final int ITEM_INPUT_X = 48;
    private static final int ITEM_INPUT_Y = 22;
    private static final int ITEM_OUTPUT_X = 133;
    private static final int ITEM_OUTPUT_Y = 22;

    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawableAnimated progress;

    public FusionConversionRecipeCategory(IJeiHelpers helpers) {
        IGuiHelper guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createDrawable(BACKGROUND, 0, 0, 175, 98);
        this.icon = guiHelper.createDrawableItemStack(controllerIcon());
        IDrawableStatic progressDrawable = guiHelper.createDrawable(BACKGROUND, 234, 0, 20, 11);
        this.progress = guiHelper.createAnimatedDrawable(progressDrawable, 60, IDrawableAnimated.StartDirection.LEFT, false);
    }

    /**
     * Dedicated catalyst icon: the Mekanism fusion reactor controller, resolved
     * by registry id so the mod still loads without Mekanism Generators.
     */
    private static net.minecraft.world.item.ItemStack controllerIcon() {
        var controller = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("mekanismgenerators", "fusion_reactor_controller"));
        if (controller != net.minecraft.world.item.Items.AIR) {
            return new net.minecraft.world.item.ItemStack(controller);
        }
        return com.raishxn.ufo.item.ModItems.DIMENSIONAL_PROCESSOR.get().getDefaultInstance();
    }

    @Override
    public RecipeType<FusionConversionRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.chaosworld_core.fusion_conversion");
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return 175;
    }

    @Override
    public int getHeight() {
        return 98;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FusionConversionRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(ITEM_INPUT_X, ITEM_INPUT_Y)
                .addIngredients(recipe.getInput());
        builder.addOutputSlot(ITEM_OUTPUT_X, ITEM_OUTPUT_Y)
                .addItemStack(recipe.getResult().copy());
    }

    @Override
    public void draw(FusionConversionRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.background.draw(guiGraphics);
        this.progress.draw(guiGraphics, 105, 42);
    }
}
