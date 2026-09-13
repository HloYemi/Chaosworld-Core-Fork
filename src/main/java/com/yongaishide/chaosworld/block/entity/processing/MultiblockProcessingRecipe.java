package com.yongaishide.chaosworld.block.entity.processing;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe;
import com.yongaishide.chaosworld.recipe.DimensionalMatterAssemblerRecipe;
import com.yongaishide.chaosworld.recipe.QMFRecipe;
import com.yongaishide.chaosworld.recipe.UniversalMultiblockRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.pedroksl.ae2addonlib.recipes.IngredientStack;

import java.util.ArrayList;
import java.util.List;

public record MultiblockProcessingRecipe(
        ResourceLocation id,
        String name,
        List<ItemRequirement> itemInputs,
        List<FluidRequirement> fluidInputs,
        List<ChemicalRequirement> chemicalInputs,
        List<OutputStack> outputs,
        long energy,
        int time,
        int requiredTier) {

    public record ItemRequirement(Ingredient ingredient, long amount) {
    }

    public record FluidRequirement(FluidStack fluid, long amount) {
    }

    public record ChemicalRequirement(ResourceLocation chemicalId, long amount) {
    }

    public record OutputStack(ItemStack item, FluidStack fluid, long amount) {
        public OutputStack {
            item = item == null ? ItemStack.EMPTY : item;
            fluid = fluid == null ? FluidStack.EMPTY : fluid;
            amount = Math.max(0L, amount);
        }
    }

    public OutputStack primaryOutput() {
        if (outputs.isEmpty()) {
            return new OutputStack(ItemStack.EMPTY, FluidStack.EMPTY, 0L);
        }
        return outputs.getFirst();
    }

    public static MultiblockProcessingRecipe fromQmf(ResourceLocation id, QMFRecipe recipe) {
        List<ItemRequirement> itemInputs = recipe.getItemInputs().stream()
                .map(input -> new ItemRequirement(input.ingredient(), input.amount()))
                .toList();
        List<FluidRequirement> fluidInputs = recipe.getFluidInputs().stream()
                .map(input -> new FluidRequirement(input.fluid(), input.amount()))
                .toList();
        List<ChemicalRequirement> chemicalInputs = recipe.getChemicalInputs().stream()
                .map(input -> new ChemicalRequirement(input.chemicalId(), input.amount()))
                .toList();
        List<OutputStack> outputs = List.of(new OutputStack(normalizeItem(recipe.getResultItem()), FluidStack.EMPTY, recipe.getResultItem().getCount()));
        return new MultiblockProcessingRecipe(id, recipe.getRecipeName(), itemInputs, fluidInputs, chemicalInputs, outputs, recipe.getEnergy(), recipe.getTime(), recipe.getRequiredTier());
    }

    public static MultiblockProcessingRecipe fromUniversal(ResourceLocation id, UniversalMultiblockRecipe recipe) {
        List<ItemRequirement> itemInputs = recipe.getItemInputs().stream()
                .map(input -> new ItemRequirement(input.ingredient(), input.amount()))
                .toList();
        List<FluidRequirement> fluidInputs = recipe.getFluidInputs().stream()
                .map(input -> new FluidRequirement(input.fluid(), input.amount()))
                .toList();
        List<ChemicalRequirement> chemicalInputs = recipe.getChemicalInputs().stream()
                .map(input -> new ChemicalRequirement(input.chemicalId(), input.amount()))
                .toList();

        List<OutputStack> outputs = new ArrayList<>();
        if (!recipe.getItemOutput().isEmpty()) {
            outputs.add(new OutputStack(normalizeItem(recipe.getItemOutput()), FluidStack.EMPTY, recipe.getItemOutputAmount()));
        }
        if (!recipe.getFluidOutput().isEmpty() && recipe.getFluidOutputAmount() > 0) {
            outputs.add(new OutputStack(ItemStack.EMPTY, recipe.getFluidOutput(), recipe.getFluidOutputAmount()));
        }

        return new MultiblockProcessingRecipe(id, recipe.getRecipeName(), itemInputs, fluidInputs, chemicalInputs, outputs, recipe.getEnergy(), recipe.getTime(), recipe.getRequiredTier());
    }

    public static MultiblockProcessingRecipe fromOverload(ResourceLocation id, OverloadProcessingRecipe recipe) {
        return fromUniversal(id, UniversalMultiblockRecipe.fromOverload(id, recipe));
    }

    /**
     * Maps a Dimensional Matter Assembler recipe onto the Quantum Matter Fabricator,
     * scaling all inputs, outputs and energy cost by {@link UniversalMultiblockRecipe#QMF_DMA_MULTIPLIER}.
     */
    public static MultiblockProcessingRecipe fromDma(ResourceLocation id, DimensionalMatterAssemblerRecipe recipe) {
        List<ItemRequirement> itemInputs = recipe.getItemInputs().stream()
                .filter(input -> input != null && !input.isEmpty())
                .map(input -> new ItemRequirement(input.getIngredient(),
                        Math.max(1L, input.getAmount()) * UniversalMultiblockRecipe.QMF_DMA_MULTIPLIER))
                .toList();

        List<FluidRequirement> fluidInputs = recipe.getFluidInputs().stream()
                .filter(input -> input != null && !input.isEmpty())
                .map(input -> {
                    FluidStack[] stacks = input.getIngredient().getStacks();
                    FluidStack fluid = stacks.length > 0 ? stacks[0] : FluidStack.EMPTY;
                    return new FluidRequirement(fluid,
                            Math.max(1L, input.getAmount()) * UniversalMultiblockRecipe.QMF_DMA_MULTIPLIER);
                })
                .filter(requirement -> !requirement.fluid().isEmpty())
                .toList();

        List<OutputStack> outputs = new ArrayList<>();
        for (var output : recipe.getItemOutputs()) {
            if (output.what() instanceof AEItemKey itemKey) {
                outputs.add(new OutputStack(itemKey.toStack(1), FluidStack.EMPTY,
                        Math.max(0L, output.amount()) * UniversalMultiblockRecipe.QMF_DMA_MULTIPLIER));
            }
        }
        for (var output : recipe.getFluidOutputs()) {
            if (output.what() instanceof AEFluidKey fluidKey) {
                outputs.add(new OutputStack(ItemStack.EMPTY, fluidKey.toStack(1),
                        Math.max(0L, output.amount()) * UniversalMultiblockRecipe.QMF_DMA_MULTIPLIER));
            }
        }

        return new MultiblockProcessingRecipe(id, "universal/qmf/dma/" + id.getPath(), itemInputs, fluidInputs,
                List.of(), outputs,
                Math.max(0L, (long) recipe.getEnergy()) * UniversalMultiblockRecipe.QMF_DMA_MULTIPLIER,
                recipe.getTime(), 1);
    }

    /**
     * Maps an ExtendedAE Circuit Cutter recipe for the Quantum Slicer machine.
     * Quantity is scaled x64000, runtime x1000 and energy x64000.
     */
    public static MultiblockProcessingRecipe fromCircuitCutter(ResourceLocation id,
            com.glodblock.github.extendedae.recipe.CircuitCutterRecipe recipe) {
        List<ItemRequirement> itemInputs = new ArrayList<>();
        com.glodblock.github.glodium.recipe.stack.IngredientStack.Item input = recipe.getInput();
        if (input != null && input.getIngredient() != null) {
            itemInputs.add(new ItemRequirement(input.getIngredient(),
                    Math.max(1L, input.getAmount()) * 64_000L));
        }
        ItemStack output = recipe.output;
        List<OutputStack> outputs = List.of(new OutputStack(normalizeItem(output), FluidStack.EMPTY,
                Math.max(1L, output.getCount()) * 64_000L));
        return new MultiblockProcessingRecipe(id, id.getPath(), itemInputs, List.of(), List.of(), outputs,
                50_000L * 64_000L, 100 * 1000, 1);
    }

    private static ItemStack normalizeItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!copy.isEmpty()) {
            copy.setCount(1);
        }
        return copy;
    }
}
