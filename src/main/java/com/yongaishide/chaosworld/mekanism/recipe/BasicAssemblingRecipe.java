package com.yongaishide.chaosworld.mekanism.recipe;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@NothingNullByDefault
public class BasicAssemblingRecipe extends AssemblingRecipe {

    protected final List<ItemInput> itemInputs;
    protected final ChemicalStackIngredient chemicalInput;
    protected final List<FluidStackIngredient> fluidInputs;
    protected final ItemStack output;

    /**
     * @param itemInputs     3x3 网格中所需的材料(无序,1~9 种,带每份消耗数量)
     * @param chemicalInput  所需化学物
     * @param fluidInputs    所需流体(0~2 种,按槽位顺序)
     * @param output         输出
     */
    public BasicAssemblingRecipe(List<ItemInput> itemInputs, ChemicalStackIngredient chemicalInput,
          List<FluidStackIngredient> fluidInputs, ItemStack output) {
        Objects.requireNonNull(itemInputs, "Item inputs cannot be null.");
        if (itemInputs.isEmpty() || itemInputs.size() > MAX_GRID_INPUTS) {
            throw new IllegalArgumentException("Item inputs must contain between 1 and " + MAX_GRID_INPUTS + " ingredients.");
        }
        this.itemInputs = List.copyOf(itemInputs);
        this.chemicalInput = Objects.requireNonNull(chemicalInput, "Chemical input cannot be null.");
        Objects.requireNonNull(fluidInputs, "Fluid inputs cannot be null.");
        if (fluidInputs.size() > MAX_FLUID_INPUTS) {
            throw new IllegalArgumentException("Fluid inputs must contain at most " + MAX_FLUID_INPUTS + " ingredients.");
        }
        this.fluidInputs = List.copyOf(fluidInputs);
        Objects.requireNonNull(output, "Output cannot be null.");
        if (output.isEmpty()) {
            throw new IllegalArgumentException("Output cannot be empty.");
        }
        this.output = output.copy();
    }

    /**
     * 无流体需求的便捷构造。
     */
    public BasicAssemblingRecipe(List<ItemInput> itemInputs, ChemicalStackIngredient chemicalInput, ItemStack output) {
        this(itemInputs, chemicalInput, List.of(), output);
    }

    @Override
    public List<ItemInput> getItemInputsWithCount() {
        return itemInputs;
    }

    @Override
    public ChemicalStackIngredient getChemicalInput() {
        return chemicalInput;
    }

    @Override
    public List<FluidStackIngredient> getFluidInputs() {
        return fluidInputs;
    }

    @Contract(pure = true)
    @Override
    public ItemStack getOutput() {
        return output.copy();
    }

    @NotNull
    @Override
    public ItemStack getResultItem(@NotNull HolderLookup.Provider provider) {
        return output.copy();
    }

    @Override
    public List<ItemStack> getOutputDefinition() {
        return Collections.singletonList(output);
    }

    /**
     * 仅供序列化器使用。请勿修改返回的堆叠!
     *
     * @return 未拷贝的输出定义
     */
    public ItemStack getOutputRaw() {
        return this.output;
    }

    @Override
    public RecipeSerializer<? extends BasicAssemblingRecipe> getSerializer() {
        return AssemblingRecipeSerializer.INSTANCE;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BasicAssemblingRecipe other = (BasicAssemblingRecipe) o;
        return itemInputs.equals(other.itemInputs) && chemicalInput.equals(other.chemicalInput)
              && fluidInputs.equals(other.fluidInputs) && ItemStack.matches(output, other.output);
    }

    @Override
    public int hashCode() {
        int hash = itemInputs.hashCode();
        hash = 31 * hash + chemicalInput.hashCode();
        hash = 31 * hash + fluidInputs.hashCode();
        hash = 31 * hash + ItemStack.hashItemAndComponents(output);
        hash = 31 * hash + output.getCount();
        return hash;
    }
}
