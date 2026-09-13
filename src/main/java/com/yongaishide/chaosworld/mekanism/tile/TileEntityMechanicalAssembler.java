package com.yongaishide.chaosworld.mekanism.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import com.yongaishide.chaosworld.mekanism.MekanismMachines;
import com.yongaishide.chaosworld.mekanism.recipe.AssemblingRecipe;
import com.yongaishide.chaosworld.mekanism.recipe.ChemicalInputCache;
import mekanism.api.Action;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.inputs.ILongInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.recipes.outputs.OutputHelper;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.fluid.FluidTankHelper;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.lookup.IRecipeLookupHandler.IRecipeTypedLookupHandler;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityProgressMachine;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 机械组装机方块实体:3x3 物品网格 + 化学物 + 能量 -> 物品。
 */
public class TileEntityMechanicalAssembler extends TileEntityProgressMachine<AssemblingRecipe>
      implements IRecipeTypedLookupHandler<AssemblingRecipe, ChemicalInputCache<AssemblingRecipe>> {

    private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
          RecipeError.NOT_ENOUGH_ENERGY,
          RecipeError.NOT_ENOUGH_INPUT,
          RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
          RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT
    );
    /** 单线程基础罐容量:64,000 mB(64 桶) */
    public static final long BASE_MB_CAPACITY = 64_000L;
    public static final int BASE_TICKS_REQUIRED = 10 * SharedConstants.TICKS_PER_SECOND;
    public static final int GRID_SIZE = 9;
    public static final int FLUID_TANK_COUNT = 2;

    //3x3 网格槽位坐标(与气体/流体区、能量区顶部对齐的同一水平带)
    //GRID_SLOT_X 为组装机专用;GRID_SLOT_Y 包内可见,锻炉子类覆写 getInitialInventory 时复用
    static final int[] GRID_SLOT_X = {73, 91, 109, 73, 91, 109, 73, 91, 109};
    static final int[] GRID_SLOT_Y = {16, 16, 16, 34, 34, 34, 52, 52, 52};

    public IChemicalTank inputTank;
    public IExtendedFluidTank inputFluidTank1;
    public IExtendedFluidTank inputFluidTank2;

    private final IOutputHandler<@NotNull ItemStack> outputHandler;
    private final ILongInputHandler<@NotNull ChemicalStack> inputHandler;
    //注意:super() 构造器会回调 getInitialInventory(),此时字段初始化尚未执行,
    //因此 gridSlots 不能依赖声明处初始化,必须在 getInitialInventory 中惰性创建
    List<IInventorySlot> gridSlots;

    private MachineEnergyContainer<TileEntityMechanicalAssembler> energyContainer;
    //包内可见:锻炉子类(龙魂锻炉)按工厂等级扩容时用 BasicInventorySlot 创建输出槽
    BasicInventorySlot outputSlot;
    EnergyInventorySlot energySlot;

    public TileEntityMechanicalAssembler(BlockPos pos, BlockState state) {
        this(MekanismMachines.MECHANICAL_ASSEMBLER, pos, state);
    }

    protected TileEntityMechanicalAssembler(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state, TRACKED_ERROR_TYPES, BASE_TICKS_REQUIRED);
        List<IInventorySlot> itemInputs = new ArrayList<>(gridSlots);
        configComponent.setupItemIOConfig(itemInputs, List.of(outputSlot), energySlot, false);
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);
        configComponent.setupInputConfig(TransmissionType.CHEMICAL, inputTank);
        //两个流体输入罐共享一条 FLUID 侧边配置
        ConfigInfo fluidConfig = configComponent.getConfig(TransmissionType.FLUID);
        if (fluidConfig != null) {
            fluidConfig.addSlotInfo(DataType.INPUT,
                  mekanism.common.tile.component.TileComponentConfig.createInfo(TransmissionType.FLUID, true, false,
                        List.of(inputFluidTank1, inputFluidTank2)));
            fluidConfig.setCanEject(false);
        }

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
        inputHandler = InputHelper.getInputHandler(inputTank, RecipeError.NOT_ENOUGH_INPUT);
        outputHandler = OutputHelper.getOutputHandler(outputSlot, RecipeError.NOT_ENOUGH_OUTPUT_SPACE);
    }

    @NotNull
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener, IContentsListener recipeCacheListener,
          IContentsListener recipeCacheUnpauseListener) {
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this);
        builder.addTank(inputTank = BasicChemicalTank.inputModern(getChemicalCapacity(), this::containsRecipe, recipeCacheListener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener, IContentsListener recipeCacheListener,
          IContentsListener recipeCacheUnpauseListener) {
        FluidTankHelper builder = FluidTankHelper.forSideWithConfig(this);
        //参数顺序:capacity, canExtract, canInsert, listener;容量 = 64,000 mB × 工厂线程数
        builder.addTank(inputFluidTank1 = BasicFluidTank.input(getFluidCapacity(), fluid -> true, this::containsFluidRecipe0, recipeCacheListener));
        builder.addTank(inputFluidTank2 = BasicFluidTank.input(getFluidCapacity(), fluid -> true, this::containsFluidRecipe1, recipeCacheListener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener, IContentsListener recipeCacheListener,
          IContentsListener recipeCacheUnpauseListener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, recipeCacheUnpauseListener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener,
          IContentsListener recipeCacheUnpauseListener) {
        if (gridSlots == null) {
            gridSlots = new ArrayList<>(GRID_SIZE);
        }
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        for (int i = 0; i < GRID_SIZE; i++) {
            //TieredInventorySlot:仿 mekextras 堆叠工厂,2^shift 扩容
            BasicInventorySlot gridSlot = builder.addSlot(new TieredInventorySlot(stack -> true, recipeCacheListener, GRID_SLOT_X[i], GRID_SLOT_Y[i], this::getStackLimitShift));
            gridSlot.tracksWarnings(slot -> slot.warning(WarningType.NO_MATCHING_RECIPE, getWarningCheck(RecipeError.NOT_ENOUGH_INPUT)));
            gridSlots.add(gridSlot);
        }
        builder.addSlot(outputSlot = OutputInventorySlot.at(recipeCacheUnpauseListener, 151, 34))
              .tracksWarnings(slot -> slot.warning(WarningType.NO_SPACE_IN_OUTPUT, getWarningCheck(RecipeError.NOT_ENOUGH_OUTPUT_SPACE)));
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 170, 5));
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        recipeCacheLookupMonitor.updateAndProcess();
        return sendUpdatePacket;
    }

    public int getEnergySlotX() {
        return energySlot.getGuiX();
    }

    /**
     * 3x3 网格槽位的物品堆叠上限(单机为 64,工厂按等级提高)。
     */
    protected int getGridSlotCapacity() {
        return 64;
    }

    /**
     * 槽位堆叠扩容指数(2^shift):单机为 0,工厂 = 等级指数(0~7)+ 堆叠升级数。
     */
    protected int getStackLimitShift() {
        return 0;
    }

    /**
     * 化学罐容量:基础 64,000 mB × 工厂线程数(单机线程数 = 1)。
     */
    public long getChemicalCapacity() {
        return BASE_MB_CAPACITY * getProcessCount();
    }

    /**
     * 流体罐容量:基础 64,000 mB × 工厂线程数(单机线程数 = 1)。
     */
    public int getFluidCapacity() {
        return (int) (BASE_MB_CAPACITY * getProcessCount());
    }

    /**
     * 生产线线程数:单机为 1,工厂按方块等级(3/5/7/9/11/13/15/17)。
     */
    protected int getProcessCount() {
        return 1;
    }

    /**
     * 化学物罐灌注谓词:按化学物类型/含量判断是否有配方。
     */
    public boolean containsRecipe(ChemicalStack stack) {
        return getRecipeType().getInputCache().containsInput(getLevel(), stack);
    }

    public boolean containsFluidRecipe0(FluidStack stack) {
        return getRecipeType().getInputCache().containsFluidInput(getLevel(), 0, stack);
    }

    public boolean containsFluidRecipe1(FluidStack stack) {
        return getRecipeType().getInputCache().containsFluidInput(getLevel(), 1, stack);
    }

    /**
     * 子类(锻炉)的缓存配方需要直接访问网格槽位以消耗材料。
     */
    protected List<IInventorySlot> getGridSlots() {
        return gridSlots;
    }

    /**
     * 3x3 网格当前物品快照。
     */
    public List<ItemStack> getGridInputs() {
        List<ItemStack> grid = new ArrayList<>(GRID_SIZE);
        for (IInventorySlot slot : gridSlots) {
            grid.add(slot.getStack());
        }
        return grid;
    }

    /**
     * 两个流体槽当前快照。
     */
    public List<FluidStack> getFluidInputs() {
        return List.of(inputFluidTank1.getFluid(), inputFluidTank2.getFluid());
    }

    public IExtendedFluidTank getFluidTank(int index) {
        return index == 0 ? inputFluidTank1 : inputFluidTank2;
    }

    @Override
    public @NotNull IMekanismRecipeTypeProvider<RecipeInput, AssemblingRecipe, ChemicalInputCache<AssemblingRecipe>> getRecipeType() {
        return MekanismMachines.ASSEMBLING;
    }

    @Override
    public @Nullable AssemblingRecipe getRecipe(int cacheIndex) {
        return getRecipeType().getInputCache().findFirstRecipe(getLevel(), inputHandler.getInput(), getGridInputs(), getFluidInputs());
    }

    @Override
    public @NotNull CachedRecipe<AssemblingRecipe> createNewCachedRecipe(@NotNull AssemblingRecipe recipe, int cacheIndex) {
        return new AssemblingCachedRecipe(recipe, recheckAllRecipeErrors, this, outputHandler)
              .setErrorsChanged(this::onErrorsChanged)
              .setCanHolderFunction(this::canFunction)
              .setActive(this::setActive)
              .setEnergyRequirements(energyContainer::getEnergyPerTick, energyContainer)
              .setRequiredTicks(this::getTicksRequired)
              .setOnFinish(this::markForSave)
              .setOperatingTicksChanged(this::setOperatingTicks)
              .setBaselineMaxOperations(this::getOperationsPerTick);
    }

    public MachineEnergyContainer<TileEntityMechanicalAssembler> getEnergyContainer() {
        return energyContainer;
    }

    /**
     * 化学物 + 3x3 物品 -> 物品 的缓存配方,仿照 {@code OneInputCachedRecipe} 的实现模式。
     */
    private static class AssemblingCachedRecipe extends CachedRecipe<AssemblingRecipe> {

        private final TileEntityMechanicalAssembler tile;
        private final IOutputHandler<@NotNull ItemStack> outputHandler;
        @Nullable
        private ItemStack output;

        AssemblingCachedRecipe(AssemblingRecipe recipe, BooleanSupplier recheckAllErrors,
              TileEntityMechanicalAssembler tile, IOutputHandler<ItemStack> outputHandler) {
            super(recipe, recheckAllErrors);
            this.tile = tile;
            this.outputHandler = outputHandler;
        }

        @Override
        public boolean isInputValid() {
            ChemicalStack chemical = tile.inputTank.getStack();
            return !chemical.isEmpty() && recipe.testChemical(chemical) && recipe.matchesGrid(tile.getGridInputs())
                  && recipe.matchesFluids(tile.getFluidInputs());
        }

        @Override
        protected void calculateOperationsThisTick(OperationTracker tracker) {
            super.calculateOperationsThisTick(tracker);
            if (!tracker.shouldContinueChecking()) {
                return;
            }
            //化学物输入
            ChemicalStack chemical = tile.inputTank.getStack();
            if (chemical.isEmpty()) {
                tracker.addError(RecipeError.NOT_ENOUGH_INPUT);
                return;
            }
            if (!recipe.testChemical(chemical)) {
                tracker.addError(RecipeError.NOT_ENOUGH_INPUT);
                return;
            }
            //3x3 网格材料
            int itemOperations = recipe.calculateGridOperations(tile.getGridInputs());
            if (itemOperations <= 0) {
                tracker.addError(RecipeError.NOT_ENOUGH_INPUT);
                return;
            }
            tracker.updateOperations(itemOperations);
            //化学物可支撑的操作数
            tracker.updateOperations((int) Math.min(Integer.MAX_VALUE, chemical.getAmount() / recipe.getChemicalAmount()));
            //流体输入
            int fluidOperations = recipe.calculateFluidOperations(tile.getFluidInputs());
            if (fluidOperations <= 0) {
                tracker.addError(RecipeError.NOT_ENOUGH_INPUT);
                return;
            }
            tracker.updateOperations(fluidOperations);
            //输出空间
            output = recipe.getOutput();
            if (output.isEmpty()) {
                tracker.addError(RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
                return;
            }
            outputHandler.calculateOperationsCanSupport(tracker, output);
        }

        @Override
        protected void finishProcessing(int operations) {
            if (output == null || output.isEmpty()) {
                return;
            }
            //消耗网格材料:每种配方材料每份消耗 count 个,跨输入格扣减(材料可能分散在多格)
            for (com.yongaishide.chaosworld.mekanism.recipe.ItemInput input : recipe.getItemInputsWithCount()) {
                int remaining = input.count() * operations;
                for (IInventorySlot slot : tile.gridSlots) {
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
            //消耗化学物
            tile.inputTank.shrinkStack(recipe.getChemicalAmount() * operations, Action.EXECUTE);
            //消耗流体(按槽位顺序)
            for (int i = 0; i < recipe.getFluidInputs().size(); i++) {
                tile.getFluidTank(i).shrinkStack(recipe.getFluidAmount(i) * operations, Action.EXECUTE);
            }
            //产出
            outputHandler.handleOutput(output, operations);
        }
    }
}
