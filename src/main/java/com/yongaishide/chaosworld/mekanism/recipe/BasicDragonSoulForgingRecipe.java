package com.yongaishide.chaosworld.mekanism.recipe;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.yongaishide.chaosworld.mekanism.DragonSoulForgeMachines;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 龙魂锻炉配方:3x3 物品(无序,1~9 种)+ 流体(0~1 种)+ 可选催化剂(额外输入)+ 能量 -> 物品。
 * <p>
 * 锻炉没有化学槽,因此配方<b>不含化学物输入</b>;流体上限为 1(锻炉只有一个流体槽)。
 * <p>
 * 催化剂({@link CatalystInput})由配方各自声明:每份完成时按配方概率消耗,
 * 未声明催化剂输入的配方不依赖催化剂槽。
 */
@NothingNullByDefault
public class BasicDragonSoulForgingRecipe extends AssemblingRecipe {

    /** 锻炉最多支持 1 种流体输入 */
    public static final int MAX_FLUID_INPUTS = 1;

    private static final Holder<Item> DRAGON_SOUL_FORGE = DeferredHolder.create(Registries.ITEM,
          ResourceLocation.fromNamespaceAndPath("chaosworld_core", "dragon_soul_forge"));

    private final List<ItemInput> itemInputs;
    private final List<FluidStackIngredient> fluidInputs;
    private final Optional<CatalystInput> catalystInput;
    private final ItemStack output;

    /**
     * @param itemInputs    3x3 网格中所需的材料(无序,1~9 种,带每份消耗数量)
     * @param fluidInputs   所需流体(0~1 种)
     * @param catalystInput 可选催化剂输入(null 表示不依赖催化剂)
     * @param output        输出
     */
    public BasicDragonSoulForgingRecipe(List<ItemInput> itemInputs, List<FluidStackIngredient> fluidInputs,
          CatalystInput catalystInput, ItemStack output) {
        if (itemInputs.isEmpty() || itemInputs.size() > MAX_GRID_INPUTS) {
            throw new IllegalArgumentException("Item inputs must contain between 1 and " + MAX_GRID_INPUTS + " ingredients.");
        }
        if (fluidInputs.size() > MAX_FLUID_INPUTS) {
            throw new IllegalArgumentException("Dragon soul forge recipes support at most " + MAX_FLUID_INPUTS + " fluid input(s).");
        }
        this.itemInputs = List.copyOf(itemInputs);
        this.fluidInputs = List.copyOf(fluidInputs);
        this.catalystInput = Optional.ofNullable(catalystInput);
        if (output.isEmpty()) {
            throw new IllegalArgumentException("Output cannot be empty.");
        }
        this.output = output.copy();
    }

    /**
     * 无催化剂与流体需求的便捷构造。
     */
    public BasicDragonSoulForgingRecipe(List<ItemInput> itemInputs, ItemStack output) {
        this(itemInputs, List.of(), null, output);
    }

    /**
     * 无流体需求的便捷构造。
     */
    public BasicDragonSoulForgingRecipe(List<ItemInput> itemInputs, CatalystInput catalystInput, ItemStack output) {
        this(itemInputs, List.of(), catalystInput, output);
    }

    public Optional<CatalystInput> getCatalystInput() {
        return catalystInput;
    }





    @Override
    public List<ItemInput> getItemInputsWithCount() {
        return itemInputs;
    }

    /**
     * 锻炉没有化学槽:化学输入恒不可用(调用即抛异常)。化学相关的缓存逻辑会先 instanceof
     * {@link BasicAssemblingRecipe},不会触碰锻炉配方。
     */
    @Override
    public ChemicalStackIngredient getChemicalInput() {
        throw new UnsupportedOperationException("Dragon soul forge recipes do not use chemicals");
    }

    @Override
    public List<FluidStackIngredient> getFluidInputs() {
        return fluidInputs;
    }

    @Override
    public ItemStack getOutput() {
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
    public boolean isIncomplete() {
        for (ItemInput itemInput : itemInputs) {
            if (itemInput.ingredient().hasNoMatchingInstances()) {
                return true;
            }
        }
        for (FluidStackIngredient fluidInput : fluidInputs) {
            if (fluidInput.hasNoMatchingInstances()) {
                return true;
            }
        }
        if (catalystInput.isPresent() && catalystInput.get().ingredient().hasNoMatchingInstances()) {
            return true;
        }
        return false;
    }

    @Override
    public RecipeType<AssemblingRecipe> getType() {
        return DragonSoulForgeMachines.DRAGON_SOUL_FORGING;
    }

    @Override
    public String getGroup() {
        return "dragon_soul_forge";
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(DRAGON_SOUL_FORGE);
    }

    @Override
    public RecipeSerializer<BasicDragonSoulForgingRecipe> getSerializer() {
        return DragonSoulForgingRecipeSerializer.INSTANCE;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BasicDragonSoulForgingRecipe other = (BasicDragonSoulForgingRecipe) o;
        return itemInputs.equals(other.itemInputs) && fluidInputs.equals(other.fluidInputs)
              && catalystInput.equals(other.catalystInput) && ItemStack.matches(output, other.output);
    }

    @Override
    public int hashCode() {
        int hash = itemInputs.hashCode();
        hash = 31 * hash + fluidInputs.hashCode();
        hash = 31 * hash + catalystInput.hashCode();
        hash = 31 * hash + ItemStack.hashItemAndComponents(output);
        hash = 31 * hash + output.getCount();
        return hash;
    }
}
