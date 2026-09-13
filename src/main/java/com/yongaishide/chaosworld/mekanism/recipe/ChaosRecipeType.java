package com.yongaishide.chaosworld.mekanism.recipe;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.common.CommonWorldTickHandler;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.cache.IInputRecipeCache;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 自制版本的 {@link MekanismRecipeType}。
 * <p>
 * MEK 的 {@link MekanismRecipeType} 构造器是私有的(仅限 MEK 自己注册配方类型),
 * 因此附属无法直接使用它。这里复刻其核心逻辑:持有配方输入缓存 + 从配方管理器惰性查询配方。
 *
 * @param <VANILLA_INPUT> 配方输入类型
 * @param <RECIPE>        配方类型
 * @param <INPUT_CACHE>   输入缓存类型
 */
public class ChaosRecipeType<VANILLA_INPUT extends RecipeInput, RECIPE extends MekanismRecipe<VANILLA_INPUT>,
      INPUT_CACHE extends IInputRecipeCache> implements RecipeType<RECIPE>,
      IMekanismRecipeTypeProvider<VANILLA_INPUT, RECIPE, INPUT_CACHE> {

    private final ResourceLocation registryName;
    private final INPUT_CACHE inputCache;
    private List<RecipeHolder<RECIPE>> cachedRecipes = Collections.emptyList();
    @Nullable
    private Level lastLevel;

    public ChaosRecipeType(ResourceLocation registryName,
          Function<ChaosRecipeType<VANILLA_INPUT, RECIPE, INPUT_CACHE>, INPUT_CACHE> inputCacheCreator) {
        this.registryName = registryName;
        this.inputCache = inputCacheCreator.apply(this);
    }

    @Override
    public String toString() {
        return registryName.toString();
    }

    @Override
    public ResourceLocation getRegistryName() {
        return registryName;
    }

    @Override
    public MekanismRecipeType<VANILLA_INPUT, RECIPE, INPUT_CACHE> getRecipeType() {
        // MEK 的 MekanismRecipeType 无法在附属中构造;本类重写了接口中所有会调用它的默认方法
        throw new UnsupportedOperationException("ChaosRecipeType does not wrap a MekanismRecipeType");
    }

    @Override
    public INPUT_CACHE getInputCache() {
        return inputCache;
    }

    /**
     * 数据包/标签刷新时清空缓存,下次查询时重新从配方管理器加载。
     */
    public void clearCaches() {
        cachedRecipes = Collections.emptyList();
        inputCache.clear();
    }

    @NotNull
    @Override
    public List<RecipeHolder<RECIPE>> getRecipes(@Nullable Level world) {
        if (world == null) {
            return Collections.emptyList();
        }
        if (lastLevel != world) {
            //切换世界/重进存档时丢弃旧世界的配方缓存
            lastLevel = world;
            cachedRecipes = Collections.emptyList();
        }
        return getRecipes(world.getRecipeManager(), world.registryAccess());
    }

    @Override
    public List<RecipeHolder<RECIPE>> getRecipes(RecipeManager recipeManager) {
        return getRecipes(recipeManager, null);
    }

    @NotNull
    @Override
    public List<RecipeHolder<RECIPE>> getRecipes(@NotNull RecipeManager recipeManager, @Nullable RegistryAccess registryAccess) {
        if (CommonWorldTickHandler.flushTagAndRecipeCaches) {
            //MEK 在标签/数据包更新后设置该标志位,借此时机清除缓存的配方
            cachedRecipes = Collections.emptyList();
        }
        if (cachedRecipes.isEmpty()) {
            cachedRecipes = recipeManager.getAllRecipesFor(this).stream()
                  .filter(recipe -> !recipe.value().isIncomplete())
                  .toList();
        }
        return cachedRecipes;
    }
}
