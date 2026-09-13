package com.yongaishide.chaosworld.mekanism.recipe;

import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.common.recipe.lookup.cache.IInputRecipeCache;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * 化学物 + 3x3 物品网格的配方缓存。
 * <p>
 * 以世界实例为键缓存配方列表;MEK 的 {@code flushTagAndRecipeCaches} 标志位被设置时,
 * {@link ChaosRecipeType#getRecipes(Level)} 内部会自动重查配方管理器。
 */
public class ChemicalInputCache<RECIPE extends MekanismRecipe<RecipeInput>>
      implements IInputRecipeCache {

    private final ChaosRecipeType<RecipeInput, RECIPE, ChemicalInputCache<RECIPE>> recipeType;
    private List<RecipeHolder<RECIPE>> recipes = List.of();
    private Level cachedLevel;

    public ChemicalInputCache(ChaosRecipeType<RecipeInput, RECIPE, ChemicalInputCache<RECIPE>> recipeType) {
        this.recipeType = recipeType;
    }

    @Override
    public void clear() {
        recipes = List.of();
        cachedLevel = null;
    }

    /**
     * 罐子灌注谓词:仅按化学物类型/含量判断是否有配方(物品网格未就绪时也允许灌气)。
     * 注意:只有化学系配方(BasicAssemblingRecipe)才参与化学判定,锻炉配方无化学输入。
     */
    public boolean containsInput(Level level, ChemicalStack stack) {
        for (RecipeHolder<RECIPE> holder : getRecipes(level)) {
            if (holder.value() instanceof BasicAssemblingRecipe recipe && recipe.testChemical(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找化学物、3x3 网格与流体槽同时匹配的配方。
     */
    @Nullable
    public RECIPE findFirstRecipe(Level level, ChemicalStack chemical, List<ItemStack> grid, List<FluidStack> fluids) {
        for (RecipeHolder<RECIPE> holder : getRecipes(level)) {
            if (holder.value() instanceof BasicAssemblingRecipe recipe
                  && recipe.testChemical(chemical) && recipe.matchesGrid(grid) && recipe.matchesFluids(fluids)) {
                return holder.value();
            }
        }
        return null;
    }

    /**
     * 流体罐灌注谓词:按槽位索引检查该槽的流体是否可能被配方使用(网格/化学物未就绪时也允许灌液)。
     */
    public boolean containsFluidInput(Level level, int index, FluidStack stack) {
        for (RecipeHolder<RECIPE> holder : getRecipes(level)) {
            if (holder.value() instanceof AssemblingRecipe recipe
                  && index < recipe.getFluidInputs().size() && recipe.getFluidInputs().get(index).test(stack)) {
                return true;
            }
        }
        return false;
    }

    private List<RecipeHolder<RECIPE>> getRecipes(Level level) {
        if (cachedLevel != level) {
            cachedLevel = level;
            recipes = recipeType.getRecipes(level);
        }
        return recipes;
    }
}
