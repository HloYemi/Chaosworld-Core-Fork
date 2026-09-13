package com.yongaishide.chaosworld.mekanism.tile;

import java.util.List;
import com.yongaishide.chaosworld.mekanism.DragonSoulForgeMachines;
import com.yongaishide.chaosworld.mekanism.recipe.AssemblingRecipe;
import com.yongaishide.chaosworld.mekanism.recipe.BasicDragonSoulForgingRecipe;
import com.yongaishide.chaosworld.mekanism.recipe.CatalystInput;
import com.yongaishide.chaosworld.mekanism.recipe.ChemicalInputCache;
import mekanism.api.Action;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.recipes.outputs.OutputHelper;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.fluid.FluidTankHelper;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.TileComponentConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 龙魂锻炉方块实体:3x3 物品 + 流体(1 槽)+ 催化剂(额外输入)+ 能量 -> 物品。
 * <p>
 * 与电路组装机不同:<b>没有化学槽</b>,且只有 <b>1 个流体槽</b>;
 * 配方类型独立为 {@code dragon_soul_forging}(配方无化学输入)。
 * <p>
 * 催化剂:放在进度条下方的催化剂槽中,作为<b>额外输入</b>——由配方各自声明
 * ({@link CatalystInput}:种类、数量、每份完成时的消耗概率)。
 * <b>硬性依赖</b>:缺少/数量不足时机器停摆;无任何速度加成;
 * 每完成一份配方 roll 一次概率,命中才消耗,未命中则保留继续使用。
 */
public class TileEntityDragonSoulForge extends TileEntityMechanicalAssembler {

    //锻炉只有 1 个流体槽:内容整体左移 32px(网格 41 起),右列留出能量组件空间
    private static final int[] FORGE_GRID_SLOT_X = {41, 59, 77, 41, 59, 77, 41, 59, 77};
    private static final int OUTPUT_SLOT_X = 119;
    //176 标准宽度下能量槽内移,避免超出窗口右缘
    private static final int ENERGY_SLOT_X = 155;
    //催化剂槽:进度条(97,34,宽 10)正下方,紧贴进度条底部
    private static final int CATALYST_SLOT_X = 99;
    private static final int CATALYST_SLOT_Y = 59;
    //===== 工厂列式布局(同 mekmm 高级工厂):上 N 格输入 → 箭头行 → 下排[催化 + N 格输出] =====
    //整体下移 10px 避开标题文字;流体槽为 18x18 小表,在催化格正上方
    private static final int FACTORY_INPUT_Y = 18;
    private static final int FACTORY_OUTPUT_Y = 62;
    private static final int FACTORY_ARROW_Y = 40;
    /** 工厂 GUI 左侧留白 */
    private static final int FACTORY_LEFT_MARGIN = 8;
    /** 工厂 GUI 右侧能量区(槽+能量条)留白 */
    private static final int FACTORY_RIGHT_MARGIN = 44;

    private BasicInventorySlot catalystSlot;
    /** 工厂版每线程一个额外输出槽(列式,与 mekmm 高级工厂一致);单机为空。
     * 注意:getInitialInventory 在 super 构造链中被调用,字段必须惰性创建。 */
    private java.util.List<BasicInventorySlot> extraOutputSlots;

    public TileEntityDragonSoulForge(BlockPos pos, BlockState state) {
        this(DragonSoulForgeMachines.DRAGON_SOUL_FORGE, pos, state);
    }

    protected TileEntityDragonSoulForge(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        //基类在构造器里为 FLUID 侧配置挂了两个罐,锻炉只有一个:重建槽位信息,替换掉 tank2
        ConfigInfo fluidConfig = configComponent.getConfig(TransmissionType.FLUID);
        if (fluidConfig != null) {
            fluidConfig.addSlotInfo(DataType.INPUT,
                  TileComponentConfig.createInfo(TransmissionType.FLUID, true, false, List.of(inputFluidTank1)));
            fluidConfig.setCanEject(false);
        }
        //物品 IO 重建:输出列表含全部列式输出槽(工厂版 N 格),输入仍为 3x3 网格
        if (outputSlot != null) {
            List<IInventorySlot> ioInputs = new java.util.ArrayList<>(getGridSlots());
            List<IInventorySlot> ioOutputs = new java.util.ArrayList<>();
            ioOutputs.add(outputSlot);
            ioOutputs.addAll(extraOutputSlots);
            configComponent.setupItemIOConfig(ioInputs, ioOutputs, energySlot, false);
        }
        //能量默认各个面输入
        ConfigInfo energyConfig = configComponent.getConfig(TransmissionType.ENERGY);
        if (energyConfig != null) {
            for (mekanism.api.RelativeSide side : mekanism.api.RelativeSide.values()) {
                energyConfig.setDataType(DataType.INPUT, side);
            }
        }
        //催化剂槽单独挂 ITEM 侧配置的 EXTRA(额外)槽位,不与 3x3 网格的 INPUT 冲突:
        //在侧配置 UI 里把某个面循环为「额外」后,该面的管道/物流即可插入(不抽出)催化剂
        mekanism.common.tile.component.config.ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null && catalystSlot != null) {
            itemConfig.addSlotInfo(DataType.EXTRA,
                  TileComponentConfig.createInfo(TransmissionType.ITEM, true, false, List.of(catalystSlot)));
        }
    }

    /**
     * 锻炉无化学槽:仅创建占位化学罐(基类构造链需要非空引用),不注册到任何容器/能力。
     * 机器类型未启用 CHEMICAL 传输,基类的化学侧配置注册自动跳过。
     */
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener, IContentsListener recipeCacheListener,
          IContentsListener recipeCacheUnpauseListener) {
        inputTank = BasicChemicalTank.input(getChemicalCapacity(), chemical -> false, recipeCacheListener);
        return ChemicalTankHelper.forSideWithConfig(this).build();
    }

    /**
     * 锻炉配方只有 1 个流体槽:只注册 tank1;tank2 仅为基类构造链占位,不注册、不参与配方。
     */
    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener, IContentsListener recipeCacheListener,
          IContentsListener recipeCacheUnpauseListener) {
        FluidTankHelper builder = FluidTankHelper.forSideWithConfig(this);
        builder.addTank(inputFluidTank1 = BasicFluidTank.input(getFluidCapacity(), fluid -> true, this::containsFluidRecipe0, recipeCacheListener));
        inputFluidTank2 = BasicFluidTank.input(getFluidCapacity(), fluid -> true, this::containsFluidRecipe1, recipeCacheListener);
        return builder.build();
    }

    /**
     * 3x3 网格 + 输出槽 + 能量槽(与组装机一致)+ 催化剂槽(进度条下方,EXTRA 类型,不参与物品侧配置)。
     */
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener,
          IContentsListener recipeCacheUnpauseListener) {
        if (gridSlots == null) {
            gridSlots = new java.util.ArrayList<>(GRID_SIZE);
        }
        if (extraOutputSlots == null) {
            extraOutputSlots = new java.util.ArrayList<>();
        }
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        boolean factory = isFactoryLayout();
        int rowX = factory ? getFactoryRowX() : 0;
        //→ 工厂列式:输入行 = N 格横向(每线程一格,从第 2 列起,第 1 列留流体/催化);单机:3x3 网格
        for (int i = 0; i < getInputSlotCount(); i++) {
            int slotX = factory ? rowX + 18 + i * 18 : FORGE_GRID_SLOT_X[i];
            int slotY = factory ? FACTORY_INPUT_Y : GRID_SLOT_Y[i];
            BasicInventorySlot gridSlot = builder.addSlot(new TieredInventorySlot(stack -> true, recipeCacheListener, slotX, slotY, this::getStackLimitShift));
            gridSlot.tracksWarnings(slot -> slot.warning(WarningType.NO_MATCHING_RECIPE, getWarningCheck(RecipeError.NOT_ENOUGH_INPUT)));
            gridSlots.add(gridSlot);
        }
        //输出槽:工厂版 = 第二行 [催化 + N 格输出] 中的首个输出格;单机沿用原坐标
        BasicInventorySlot out = builder.addSlot(new TieredInventorySlot(
              stack -> true, recipeCacheUnpauseListener, factory ? rowX + 18 : OUTPUT_SLOT_X,
              factory ? FACTORY_OUTPUT_Y : 34, this::getStackLimitShift));
        out.setSlotType(ContainerSlotType.OUTPUT);
        outputSlot = out;
        out.tracksWarnings(slot -> slot.warning(WarningType.NO_SPACE_IN_OUTPUT, getWarningCheck(RecipeError.NOT_ENOUGH_OUTPUT_SPACE)));
        //能量槽:工厂版放到输入行最左(第 1 列第 1 格,流体/催化剂正上方);单机沿用原坐标
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(getEnergyContainer(), this::getLevel, listener,
              factory ? rowX : ENERGY_SLOT_X, factory ? FACTORY_INPUT_Y : 5));
        //催化剂槽:工厂版 = 第二行最左列(18x18 流体槽在其正上方);单机沿用原坐标
        BasicInventorySlot catalyst = builder.addSlot(new TieredInventorySlot(
              stack -> true, listener,
              factory ? rowX : CATALYST_SLOT_X,
              factory ? FACTORY_OUTPUT_Y : CATALYST_SLOT_Y,
              this::getStackLimitShift));
        catalyst.setSlotType(ContainerSlotType.EXTRA);
        catalystSlot = catalyst;
        //工厂版:其余线程输出格(第二行 N-1 格,排成一行输出)
        for (int i = 0; i < getExtraOutputSlotCount(); i++) {
            BasicInventorySlot extra = builder.addSlot(new TieredInventorySlot(
                  stack -> true, recipeCacheUnpauseListener, rowX + (i + 2) * 18, FACTORY_OUTPUT_Y, this::getStackLimitShift));
            extra.setSlotType(ContainerSlotType.OUTPUT);
            extra.tracksWarnings(slot -> slot.warning(WarningType.NO_SPACE_IN_OUTPUT, getWarningCheck(RecipeError.NOT_ENOUGH_OUTPUT_SPACE)));
            extraOutputSlots.add(extra);
        }
        return builder.build();
    }

    /** 工厂版额外(列式)输出槽数量;单机为 0 */
    protected int getExtraOutputSlotCount() {
        return 0;
    }

    /** 输入槽数量:单机 3x3 = 9;工厂 = 线程数(每线程一格) */
    protected int getInputSlotCount() {
        return GRID_SIZE;
    }

    /** 工厂列式布局的线程数;单机(非工厂)返回 1 */
    public int getThreadCount() {
        return getInputSlotCount() != GRID_SIZE ? getInputSlotCount() : 1;
    }

    /** 是否处于工厂列式布局 */
    public boolean isFactoryLayout() {
        return getInputSlotCount() != GRID_SIZE;
    }

    /** 第二行像素宽:[催化剂 + N 输出] 格 */
    public int getFactoryRowWidth() {
        return (getThreadCount() + 1) * 18;
    }

    /** 工厂版 GUI 窗口宽:行宽 + 左右留白(含能量区),最小 176(物品栏对齐) */
    public int getFactoryGuiWidth() {
        return Math.max(176, getFactoryRowWidth() + FACTORY_LEFT_MARGIN + FACTORY_RIGHT_MARGIN);
    }

    /** 行起点 X:左贴背景(同 mekmm,行从左排到右;玩家物品栏另行居中) */
    public int getFactoryRowX() {
        return FACTORY_LEFT_MARGIN;
    }



    /** 所有输出槽(主槽 + 工厂列式槽) */
    public java.util.List<BasicInventorySlot> getAllOutputSlots() {
        java.util.List<BasicInventorySlot> all = new java.util.ArrayList<>(1 + (extraOutputSlots == null ? 0 : extraOutputSlots.size()));
        all.add(outputSlot);
        if (extraOutputSlots != null) {
            all.addAll(extraOutputSlots);
        }
        return all;
    }

    public BasicInventorySlot getCatalystSlot() {
        return catalystSlot;
    }


    /**
     * 当前催化剂槽中与配方匹配的数量(物品匹配才计数,否则按 0 处理)。
     */
    public int getCatalystCount() {
        return catalystSlot == null ? 0 : catalystSlot.getCount();
    }

    /**
     * 催化剂是否满足当前配方(必须匹配物品且数量 ≥ amount;配方未声明时恒 true)。
     */
    public boolean matchesRecipeCatalyst(BasicDragonSoulForgingRecipe recipe) {
        if (recipe.getCatalystInput().isEmpty()) {
            return true;
        }
        if (catalystSlot == null || catalystSlot.isEmpty()) {
            return false;
        }
        CatalystInput catalyst = recipe.getCatalystInput().get();
        return catalyst.ingredient().test(catalystSlot.getStack()) && catalystSlot.getCount() >= catalyst.amount();
    }

    /**
     * 催化剂消耗(按配方二选一):consume=true 时每轮批量(一次配方完成)固定消耗 amount 个;
     * consume=false 时不消耗,催化剂仅作运行门槛反复使用。
     * 消耗按"轮"而非"每份配方",不限制工厂并行份数。
     */
    public void consumeCatalyst(CatalystInput catalyst, int operations) {
        if (catalyst == null || !catalyst.consume() || catalystSlot == null || catalystSlot.isEmpty()
              || !catalyst.ingredient().test(catalystSlot.getStack())) {
            return;
        }
        int toConsume = Math.min(catalyst.amount(), catalystSlot.getCount());
        if (toConsume > 0) {
            catalystSlot.shrinkStack(toConsume, Action.EXECUTE);
            setChanged();
        }
    }

    /**
     * 锻炉配方最多 1 个流体输入(与 1 个流体槽对应)。
     */
    @Override
    public List<FluidStack> getFluidInputs() {
        return List.of(inputFluidTank1.getFluid());
    }

    @Override
    public IExtendedFluidTank getFluidTank(int index) {
        return inputFluidTank1;
    }

    /**
     * 锻炉配方不含化学物:按物品网格 + 流体槽 + 催化剂匹配。
     * 催化剂是硬性输入,不足时返回 null(配方缓存不再继续生产)。
     */
    @Override
    public @Nullable AssemblingRecipe getRecipe(int cacheIndex) {
        for (RecipeHolder<AssemblingRecipe> holder : getRecipeType().getRecipes(getLevel())) {
            if (holder.value() instanceof BasicDragonSoulForgingRecipe recipe
                  && recipe.matchesGrid(getGridInputs()) && recipe.matchesFluids(getFluidInputs())
                  && matchesRecipeCatalyst(recipe)) {
                return recipe;
            }
        }
        //催化剂不足时无配方可缓存,机器停摆
        return null;
    }

    @Override
    public @NotNull CachedRecipe<AssemblingRecipe> createNewCachedRecipe(@NotNull AssemblingRecipe recipe, int cacheIndex) {
        return new ForgeCachedRecipe(recipe, recheckAllRecipeErrors, this)
              .setErrorsChanged(this::onErrorsChanged)
              .setCanHolderFunction(this::canFunction)
              .setActive(this::setActive)
              .setEnergyRequirements(getEnergyContainer()::getEnergyPerTick, getEnergyContainer())
              .setRequiredTicks(this::getTicksRequired)
              .setOnFinish(this::markForSave)
              .setOperatingTicksChanged(this::setOperatingTicks)
              .setBaselineMaxOperations(this::getOperationsPerTick);
    }

    @Override
    public @NotNull IMekanismRecipeTypeProvider<RecipeInput, AssemblingRecipe, ChemicalInputCache<AssemblingRecipe>> getRecipeType() {
        return DragonSoulForgeMachines.DRAGON_SOUL_FORGING;
    }

    /**
     * 锻炉缓存配方:物品网格 + 流体 + 输出空间,无化学物部分。
     */
    private static class ForgeCachedRecipe extends CachedRecipe<AssemblingRecipe> {

        private final TileEntityDragonSoulForge tile;
        private final BasicDragonSoulForgingRecipe forgeRecipe;
        @Nullable
        private ItemStack output;

        ForgeCachedRecipe(AssemblingRecipe recipe, java.util.function.BooleanSupplier recheckAllErrors,
              TileEntityDragonSoulForge tile) {
            super(recipe, recheckAllErrors);
            this.tile = tile;
            this.forgeRecipe = (BasicDragonSoulForgingRecipe) recipe;
        }

        /**
         * 全部输出槽(单机 1 格,工厂 N 格列式)还能容纳的总操作数。
         */
        private int calculateOutputSpace(ItemStack outputStack) {
            long space = 0;
            for (BasicInventorySlot slot : tile.getAllOutputSlots()) {
                long fit;
                if (slot.isEmpty()) {
                    //空槽:按该槽容量计算
                    fit = slot.getLimit(outputStack) / outputStack.getCount();
                } else if (ItemStack.isSameItemSameComponents(slot.getStack(), outputStack)) {
                    int remaining = slot.getLimit(outputStack) - slot.getCount();
                    fit = remaining / outputStack.getCount();
                } else {
                    fit = 0;
                }
                space += Math.max(0, fit);
            }
            if (space <= 0) {
                return 0;
            }
            return (int) Math.min(Integer.MAX_VALUE, space);
        }

        @Override
        public boolean isInputValid() {
            //催化剂是硬性输入:缺少或数量不足时停摆
            return forgeRecipe.matchesGrid(tile.getGridInputs()) && forgeRecipe.matchesFluids(tile.getFluidInputs())
                  && tile.matchesRecipeCatalyst(forgeRecipe);
        }

        @Override
        protected void calculateOperationsThisTick(OperationTracker tracker) {
            super.calculateOperationsThisTick(tracker);
            if (!tracker.shouldContinueChecking()) {
                return;
            }
            //3x3 网格材料
            int itemOperations = forgeRecipe.calculateGridOperations(tile.getGridInputs());
            if (itemOperations <= 0) {
                tracker.updateOperations(0);
                tracker.addError(RecipeError.NOT_ENOUGH_INPUT);
                return;
            }
            tracker.updateOperations(itemOperations);
            //流体输入
            int fluidOperations = forgeRecipe.calculateFluidOperations(tile.getFluidInputs());
            if (fluidOperations <= 0) {
                tracker.updateOperations(0);
                tracker.addError(RecipeError.NOT_ENOUGH_INPUT);
                return;
            }
            tracker.updateOperations(fluidOperations);
            //催化剂不限制并行份数:资格由 isInputValid/getRecipe 保证(缺了就停摆),
            //每轮批量(finishProcessing)按配方 amount 消耗/保留
            //输出空间:全部输出槽(单机 1 格,工厂 N 格列式)
            output = forgeRecipe.getOutput();
            if (output.isEmpty()) {
                tracker.updateOperations(0);
                tracker.addError(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
                return;
            }
            int outputSpace = calculateOutputSpace(output);
            if (outputSpace <= 0) {
                tracker.updateOperations(0);
                tracker.addError(RecipeError.NOT_ENOUGH_OUTPUT_SPACE);
                return;
            }
            tracker.updateOperations(outputSpace);
        }

        @Override
        protected void finishProcessing(int operations) {
            if (output == null || output.isEmpty()) {
                return;
            }
            //消耗网格材料:每种配方材料每份消耗 count 个,跨输入格扣减(材料可能分散在多格)
            for (com.yongaishide.chaosworld.mekanism.recipe.ItemInput input : forgeRecipe.getItemInputsWithCount()) {
                int remaining = input.count() * operations;
                for (IInventorySlot slot : tile.getGridSlots()) {
                    if (remaining <= 0) {
                        break;
                    }
                    if (!slot.isEmpty() && input.ingredient().test(slot.getStack())) {
                        int take = Math.min(remaining, slot.getCount());
                        slot.shrinkStack(take, Action.EXECUTE);
                        remaining -= take;
                    }
                }
            }
            //消耗流体
            for (int i = 0; i < forgeRecipe.getFluidInputs().size(); i++) {
                tile.getFluidTank(i).shrinkStack(forgeRecipe.getFluidAmount(i) * operations, Action.EXECUTE);
            }
            //催化剂:按配方概率消耗(每份操作一次 roll)
            forgeRecipe.getCatalystInput().ifPresent(catalyst -> tile.consumeCatalyst(catalyst, operations));
            //产出
            //产出:逐格塞入全部输出槽(工厂版 N 格列式,单机 1 格)
            ItemStack toInsert = output.copy();
            toInsert.setCount(Math.multiplyExact(output.getCount(), operations));
            for (BasicInventorySlot slot : tile.getAllOutputSlots()) {
                if (toInsert.isEmpty() || slot.isEmpty() && slot.getLimit(toInsert) <= 0) {
                    if (toInsert.isEmpty()) {
                        break;
                    }
                    continue;
                }
                if (!slot.isEmpty() && !ItemStack.isSameItemSameComponents(slot.getStack(), toInsert)) {
                    continue;
                }
                ItemStack leftover = slot.insertItem(toInsert, Action.EXECUTE, mekanism.api.AutomationType.INTERNAL);
                toInsert = leftover;
                if (toInsert.isEmpty()) {
                    break;
                }
            }
        }
    }
}