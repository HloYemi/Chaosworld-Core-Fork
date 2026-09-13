package com.yongaishide.chaosworld.mekanism.client.recipe_viewer;

import java.util.ArrayList;
import java.util.List;
import com.yongaishide.chaosworld.ChaosWorld;
import com.yongaishide.chaosworld.mekanism.DragonSoulForgeMachines;
import com.yongaishide.chaosworld.mekanism.recipe.BasicDragonSoulForgingRecipe;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

/**
 * 龙魂锻炉的配方查看器类型:id 与 JEI 类别(chaosworld_core:dragon_soul_forging)一致,
 * 使 GUI 进度条点击后打开锻炉自己的配方界面(而非电路组装机)。
 */
public class DragonSoulForgingRVRecipeType implements IRecipeViewerRecipeType<BasicDragonSoulForgingRecipe> {

    public static final DragonSoulForgingRVRecipeType INSTANCE = new DragonSoulForgingRVRecipeType();

    @Override
    public ResourceLocation id() {
        return ChaosWorld.id("dragon_soul_forging");
    }

    @Override
    public Class<? extends BasicDragonSoulForgingRecipe> recipeClass() {
        return BasicDragonSoulForgingRecipe.class;
    }

    @Override
    public boolean requiresHolder() {
        return false;
    }

    @Override
    public ItemStack iconStack() {
        return DragonSoulForgeMachines.DRAGON_SOUL_FORGE.asItem().getDefaultInstance();
    }

    @Override
    public @Nullable ResourceLocation icon() {
        return null;
    }

    @Override
    public int xOffset() {
        return 0;
    }

    @Override
    public int yOffset() {
        return 0;
    }

    @Override
    public int width() {
        return 172;
    }

    @Override
    public int height() {
        return 76;
    }

    @Override
    public List<ItemLike> workstations() {
        List<ItemLike> workstations = new ArrayList<>();
        workstations.add(DragonSoulForgeMachines.DRAGON_SOUL_FORGE);
        for (int i = 0; i < DragonSoulForgeMachines.getCount(); i++) {
            workstations.add(DragonSoulForgeMachines.FACTORY_BLOCKS[i]);
        }
        return workstations;
    }

    @Override
    public MutableComponent getTextComponent() {
        return Component.translatable("container.chaosworld_core.dragon_soul_forge");
    }
}
