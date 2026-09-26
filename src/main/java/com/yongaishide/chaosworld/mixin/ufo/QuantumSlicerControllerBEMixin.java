package com.yongaishide.chaosworld.mixin.ufo;

import com.raishxn.ufo.block.entity.QuantumSlicerControllerBE;
import com.raishxn.ufo.block.entity.processing.MultiblockProcessingRecipe;
import com.yongaishide.chaosworld.patch.ufo.PatchRecipes;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;

/**
 * Patch layer: the Quantum Slicer processes ExtendedAE Circuit Cutter recipes
 * scaled x64000 (UFO Future reset behaviour).
 */
@Mixin(value = QuantumSlicerControllerBE.class, remap = false)
public abstract class QuantumSlicerControllerBEMixin {

    @Overwrite(remap = false)
    protected List<MultiblockProcessingRecipe> getAvailableRecipes() {
        var level = ((BlockEntity) (Object) this).getLevel();
        if (level == null) {
            return List.of();
        }

        List<MultiblockProcessingRecipe> recipes = new ArrayList<>();
        if (net.neoforged.fml.ModList.get().isLoaded("extendedae")) {
            try {
                for (var holder : level.getRecipeManager().getAllRecipesFor(
                        com.glodblock.github.extendedae.recipe.CircuitCutterRecipe.TYPE)) {
                    recipes.add(PatchRecipes.fromCircuitCutter(holder.id(), holder.value()));
                }
            } catch (Throwable ignored) {
                // never break ticking on compat issues
            }
        }
        return recipes;
    }
}
