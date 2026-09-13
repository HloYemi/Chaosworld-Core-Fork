package com.yongaishide.chaosworld.mekanism.recipe;

import java.util.List;
import com.yongaishide.chaosworld.mekanism.MekanismMachines;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * 机械组装机配方:3x3 网格内的物品(无序匹配,每种材料每次操作消耗 1 个)+ 化学物 + 流体(0~2 种) -> 物品。
 * <p>
 * 网格匹配允许最多 9 种材料,网格中的多余物品会保留;流体输入按槽位顺序匹配。
 */
@NothingNullByDefault
public abstract class AssemblingRecipe extends MekanismRecipe<RecipeInput> {

    public static final int MAX_GRID_INPUTS = 9;
    public static final int MAX_FLUID_INPUTS = 2;

    private static final Holder<Item> MECHANICAL_ASSEMBLER = DeferredHolder.create(Registries.ITEM,
          ResourceLocation.fromNamespaceAndPath("chaosworld_core", "mechanical_assembler"));

    /**
     * 配方所需的物品材料(无序,最多 9 种,带每份消耗数量)。
     *
     * @implNote 返回的列表 <strong>不得</strong> 被修改。
     */
    public abstract List<ItemInput> getItemInputsWithCount();

    /**
     * 配方所需的物品材料(一种材料一个条目)。
     *
     * @implNote 返回的列表 <strong>不得</strong> 被修改。
     */
    public List<ItemStackIngredient> getItemInputs() {
        return getItemInputsWithCount().stream().map(ItemInput::ingredient).toList();
    }

    /**
     * 配方所需的化学物。
     */
    public abstract ChemicalStackIngredient getChemicalInput();

    /**
     * 配方所需的流体(按槽位顺序匹配,0~2 种)。
     *
     * @implNote 返回的列表 <strong>不得</strong> 被修改。
     */
    public abstract List<FluidStackIngredient> getFluidInputs();

    /**
     * 每次操作产出的物品。
     *
     * @return 输出,<strong>不得</strong> 被修改。
     */
    @Contract(pure = true)
    public abstract ItemStack getOutput();

    /**
     * 供 JEI 等使用的输出展示。
     *
     * @return 输出的展示,<strong>不得</strong> 被修改。
     */
    public abstract List<ItemStack> getOutputDefinition();

    /**
     * 每次操作消耗的化学物数量(来自化学物输入的数量)。
     */
    public long getChemicalAmount() {
        return getChemicalInput().getRepresentations().get(0).getAmount();
    }

    /**
     * 第 index 个流体槽每次操作消耗的流体数量。
     */
    public int getFluidAmount(int index) {
        return getFluidInputs().get(index).getRepresentations().get(0).getAmount();
    }

    /**
     * 计算两个流体槽当前最多还能支撑多少次操作(按槽位顺序匹配,未使用的槽位可为空)。
     *
     * @param fluids 两个流体槽的快照
     *
     * @return 最大操作数,无流体需求时返回 {@link Integer#MAX_VALUE}(不限制)
     */
    public int calculateFluidOperations(List<FluidStack> fluids) {
        List<FluidStackIngredient> inputs = getFluidInputs();
        int operations = Integer.MAX_VALUE;
        for (int i = 0; i < inputs.size(); i++) {
            FluidStackIngredient ingredient = inputs.get(i);
            FluidStack stack = i < fluids.size() ? fluids.get(i) : FluidStack.EMPTY;
            if (stack.isEmpty() || !ingredient.test(stack)) {
                return 0;
            }
            operations = Math.min(operations, (int) (stack.getAmount() / getFluidAmount(i)));
        }
        return operations;
    }

    /**
     * 流体槽中的流体能否满足配方需求。
     */
    public boolean matchesFluids(List<FluidStack> fluids) {
        return calculateFluidOperations(fluids) > 0;
    }

    public boolean testChemical(ChemicalStack stack) {
        return getChemicalInput().test(stack);
    }

    public boolean testType(ChemicalStack stack) {
        return getChemicalInput().testType(stack);
    }

    /**
     * 网格中的物品能否满足配方材料需求(每种材料至少 1 个)。
     */
    public boolean matchesGrid(List<ItemStack> grid) {
        return calculateGridOperations(grid) > 0;
    }

    /**
     * 计算当前网格最多还能支撑多少次操作(每次操作消耗每种材料各 {@code count} 个)。
     * <p>
     * 份数按<b>数量聚合</b>计算:同种材料分散在多格(或单格堆叠)时,
     * 全部格子的数量相加 ÷ 每份消耗量(一格 64 钻石 / count=2 = 32 份)。
     *
     * @param grid 输入格(单机 9 格 3x3;工厂每线程一格)的物品快照
     *
     * @return 最大操作数,材料不足时返回 0
     */
    public int calculateGridOperations(List<ItemStack> grid) {
        int operations = Integer.MAX_VALUE;
        for (ItemInput input : getItemInputsWithCount()) {
            long available = 0;
            for (ItemStack stack : grid) {
                if (!stack.isEmpty() && input.ingredient().test(stack)) {
                    available += stack.getCount();
                }
            }
            int ops = (int) (available / input.count());
            if (ops <= 0) {
                return 0;
            }
            operations = Math.min(operations, ops);
        }
        return operations == Integer.MAX_VALUE ? 0 : operations;
    }

    @NotNull
    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @NotNull
    @Override
    public ItemStack getResultItem(@NotNull HolderLookup.Provider provider) {
        return getOutput();
    }

    @Override
    public boolean isIncomplete() {
        if (getItemInputs().isEmpty() || getChemicalInput().hasNoMatchingInstances()) {
            return true;
        }
        for (FluidStackIngredient fluidInput : getFluidInputs()) {
            if (fluidInput.hasNoMatchingInstances()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public RecipeType<AssemblingRecipe> getType() {
        return MekanismMachines.ASSEMBLING;
    }

    @Override
    public String getGroup() {
        return "mechanical_assembler";
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(MECHANICAL_ASSEMBLER);
    }
}
