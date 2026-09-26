package com.yongaishide.chaosworld.mixin.ufo;

import com.raishxn.ufo.block.entity.QmfControllerBE;
import com.raishxn.ufo.block.entity.processing.MultiblockProcessingRecipe;
import com.raishxn.ufo.init.ModRecipes;
import com.raishxn.ufo.recipe.DimensionalMatterAssemblerRecipe;
import com.raishxn.ufo.recipe.QMFRecipe;
import com.raishxn.ufo.recipe.UniversalMultiblockMachineKind;
import com.raishxn.ufo.recipe.UniversalMultiblockRecipe;
import com.yongaishide.chaosworld.patch.ufo.PatchRecipes;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;

/**
 * Patch layer: QMF maps DMA recipes scaled x64 (UFO Future reset behaviour).
 */
@Mixin(value = QmfControllerBE.class, remap = false)
public abstract class QmfControllerBEMixin {

    @Overwrite(remap = false)
    protected List<MultiblockProcessingRecipe> getAvailableRecipes() {
        var level = ((BlockEntity) (Object) this).getLevel();
        if (level == null) {
            return List.of();
        }

        List<MultiblockProcessingRecipe> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.QMF_TYPE.get())) {
            recipes.add(MultiblockProcessingRecipe.fromQmf(holder.id(), (QMFRecipe) holder.value()));
        }
        for (RecipeHolder<?> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.DMA_RECIPE_TYPE.get())) {
            recipes.add(PatchRecipes.fromDma(holder.id(), (DimensionalMatterAssemblerRecipe) holder.value()));
        }
        for (RecipeHolder<?> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.UNIVERSAL_MULTIBLOCK_TYPE.get())) {
            var recipe = (UniversalMultiblockRecipe) holder.value();
            if (recipe.getMachine() == UniversalMultiblockMachineKind.QMF) {
                recipes.add(MultiblockProcessingRecipe.fromUniversal(holder.id(), recipe));
            }
        }
        return recipes;
    }
}
