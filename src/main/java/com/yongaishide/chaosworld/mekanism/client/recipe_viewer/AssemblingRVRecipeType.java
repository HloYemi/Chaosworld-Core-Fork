package com.yongaishide.chaosworld.mekanism.client.recipe_viewer;

import java.util.List;
import com.yongaishide.chaosworld.ChaosWorld;
import com.yongaishide.chaosworld.mekanism.MekanismMachines;
import com.yongaishide.chaosworld.mekanism.recipe.BasicAssemblingRecipe;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

/**
 * 机械组装机的配方查看器类型,id 与 JEI 类别(chaosworld_core:assembling)一致,
 * 使 GUI 进度条点击后能直接打开对应的 JEI 配方界面。
 */
public class AssemblingRVRecipeType implements IRecipeViewerRecipeType<BasicAssemblingRecipe> {

    public static final AssemblingRVRecipeType INSTANCE = new AssemblingRVRecipeType();

    @Override
    public ResourceLocation id() {
        return ChaosWorld.id("assembling");
    }

    @Override
    public Class<? extends BasicAssemblingRecipe> recipeClass() {
        return BasicAssemblingRecipe.class;
    }

    @Override
    public boolean requiresHolder() {
        return false;
    }

    @Override
    public ItemStack iconStack() {
        return MekanismMachines.MECHANICAL_ASSEMBLER.asItem().getDefaultInstance();
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
        return List.of(MekanismMachines.MECHANICAL_ASSEMBLER);
    }

    @Override
    public MutableComponent getTextComponent() {
        return Component.translatable("container.chaosworld_core.mechanical_assembler");
    }
}
