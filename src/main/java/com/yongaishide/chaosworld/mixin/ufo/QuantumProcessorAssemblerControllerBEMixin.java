package com.yongaishide.chaosworld.mixin.ufo;

import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.raishxn.ufo.block.entity.QuantumProcessorAssemblerControllerBE;
import com.raishxn.ufo.block.entity.processing.MultiblockProcessingRecipe;
import com.raishxn.ufo.init.ModRecipes;
import com.raishxn.ufo.recipe.UniversalMultiblockMachineKind;
import com.yongaishide.chaosworld.patch.ufo.PatchRecipes;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;

/**
 * Patch layer: the Quantum Processing Factory maps Lightning Tech overload
 * processing recipes scaled x20000 (UFO Future reset behaviour), and uses the
 * "quantum_processing_factory" display name.
 */
@Mixin(value = QuantumProcessorAssemblerControllerBE.class, remap = false)
public abstract class QuantumProcessorAssemblerControllerBEMixin {

    @Overwrite(remap = false)
    protected String getControllerTranslationKey() {
        return "block.ufo.quantum_processing_factory_controller";
    }

    @Overwrite(remap = false)
    protected List<MultiblockProcessingRecipe> getAvailableRecipes() {
        var level = ((BlockEntity) (Object) this).getLevel();
        if (level == null) {
            return List.of();
        }

        List<MultiblockProcessingRecipe> recipes = new ArrayList<>();
        var recipeManager = level.getRecipeManager();

        for (var holder : recipeManager.getAllRecipesFor(ModRecipes.UNIVERSAL_MULTIBLOCK_TYPE.get())) {
            var recipe = holder.value();
            if (recipe.getMachine() == UniversalMultiblockMachineKind.QUANTUM_PROCESSOR_ASSEMBLER) {
                recipes.add(MultiblockProcessingRecipe.fromUniversal(holder.id(), recipe));
            }
        }

        for (var holder : recipeManager.getAllRecipesFor(ModRecipeTypes.OVERLOAD_PROCESSING_TYPE.get())) {
            var recipe = holder.value();
            if (!recipe.isIncomplete()) {
                recipes.add(PatchRecipes.fromOverload(holder.id(), recipe));
            }
        }

        return recipes;
    }
}
