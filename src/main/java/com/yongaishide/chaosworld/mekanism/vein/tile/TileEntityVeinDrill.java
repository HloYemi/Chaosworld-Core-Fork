package com.yongaishide.chaosworld.mekanism.vein.tile;

import java.util.ArrayList;
import java.util.List;
import com.yongaishide.chaosworld.mekanism.vein.OreEntry;
import com.yongaishide.chaosworld.mekanism.vein.VeinData;
import com.yongaishide.chaosworld.mekanism.vein.VeinDrillDataLoader;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.tile.component.TileComponentEjector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 虚脉钻探机:能量 + 虚拟钻头(基础耐久 1000,等级提升产出)+ 虚拟矿脉(等级/质量) -> 矿石。
 * 修复铁锭每锭 +25 耐久;每 500t 一次挖掘;能耗 = 64 × 单次产出数。
 */
public class TileEntityVeinDrill extends TileEntityConfigurableMachine
      implements mekanism.common.tile.interfaces.IBoundingBlock, mekanism.common.lib.chunkloading.IChunkLoader {

    private final mekanism.common.tile.component.TileComponentChunkLoader<TileEntityVeinDrill> chunkLoaderComponent =
          new mekanism.common.tile.component.TileComponentChunkLoader<>(this);

    /** 速度升级后的挖掘周期基数(500t 起步,升级后按 MEK 公式缩短) */
    private int ticksRequired = BASE_TICK_REQUIRED;
    /** 速度升级带来的并行数(每次挖掘执行该次数,不改变单次产出堆叠) */
    private int operationsPerTick = 1;

    @Override
    public void recalculateUpgrades(mekanism.api.Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == mekanism.api.Upgrade.SPEED) {
            ticksRequired = mekanism.common.util.MekanismUtils.getTicks(this, BASE_TICK_REQUIRED);
        } else if (upgrade == com.jerry.mekextras.api.ExtraUpgrade.STACK) {
            //堆叠升级插件:这里只驱动并行数(每次挖掘执行次数 ×2^级别),不改变单格堆叠容量
            int level = getComponent().getUpgrades(upgrade);
            operationsPerTick = 1 << level;
        }
    }

    /**
     * 覆盖升级信息显示:
     * - SPEED:真实倍率 10^(升级数/8),满级 10(与实际周期 200->20t 一致)
     * - STACK(并行):实际并行 2^升级数,满级 256(而非默认的 10^(N/8))
     */
    @Override
    public java.util.List<net.minecraft.network.chat.Component> getInfo(mekanism.api.Upgrade upgrade) {
        if (upgrade == mekanism.api.Upgrade.SPEED || upgrade == com.jerry.mekextras.api.ExtraUpgrade.STACK) {
            int level = getComponent().getUpgrades(upgrade);
            double effect = upgrade == com.jerry.mekextras.api.ExtraUpgrade.STACK
                  ? Math.pow(2, level)
                  : Math.pow(10, level / (double) upgrade.getMax());
            return java.util.List.of(mekanism.common.MekanismLang.UPGRADES_EFFECT.translate(Math.round(effect * 100) / 100F));
        }
        return super.getInfo(upgrade);
    }

    @Override
    public mekanism.common.tile.component.TileComponentChunkLoader<TileEntityVeinDrill> getChunkLoader() {
        return chunkLoaderComponent;
    }

    @Override
    public java.util.Set<net.minecraft.world.level.ChunkPos> getChunkSet() {
        return java.util.Collections.singleton(new net.minecraft.world.level.ChunkPos(worldPosition));
    }

    /**
     * 边界块端口门控(与数字采矿机同思路):能量 左/右边界块侧面;物品 顶部边界块正上方/背面边界块背面。
     */
    @Override
    public boolean isOffsetCapabilityDisabled(@NotNull net.neoforged.neoforge.capabilities.BlockCapability<?, net.minecraft.core.Direction> capability,
          net.minecraft.core.Direction side, @NotNull net.minecraft.core.Vec3i offset) {
        if (capability == mekanism.common.capabilities.Capabilities.ITEM.block()) {
            return notItemPort(side, offset);
        } else if (mekanism.common.integration.energy.EnergyCompatUtils.isEnergyCapability(capability)) {
            return notEnergyPort(side, offset);
        }
        return notItemPort(side, offset) && notEnergyPort(side, offset);
    }

    private boolean notItemPort(net.minecraft.core.Direction side, net.minecraft.core.Vec3i offset) {
        if (offset.equals(new net.minecraft.core.Vec3i(0, 1, 0))) {
            //顶部输入口:仅正上面
            return side != net.minecraft.core.Direction.UP;
        }
        net.minecraft.core.Direction back = getOppositeDirection();
        if (offset.equals(new net.minecraft.core.Vec3i(back.getStepX(), 0, back.getStepZ()))
              || offset.equals(new net.minecraft.core.Vec3i(back.getStepX(), 1, back.getStepZ()))) {
            //后侧输出口:仅背面
            return side != back;
        }
        return true;
    }

    private boolean notEnergyPort(net.minecraft.core.Direction side, net.minecraft.core.Vec3i offset) {
        net.minecraft.core.Direction left = getLeftSide();
        if (offset.equals(new net.minecraft.core.Vec3i(left.getStepX(), 0, left.getStepZ()))
              || offset.equals(new net.minecraft.core.Vec3i(left.getStepX(), 1, left.getStepZ()))) {
            return side != left;
        }
        net.minecraft.core.Direction right = left.getOpposite();
        if (offset.equals(new net.minecraft.core.Vec3i(right.getStepX(), 0, right.getStepZ()))
              || offset.equals(new net.minecraft.core.Vec3i(right.getStepX(), 1, right.getStepZ()))) {
            return side != right;
        }
        return true;
    }

    public static final long BASE_DURABILITY = 1000;
    public static final int BASE_TICK_REQUIRED = 200;
    public static final long ENERGY_BASE = 256;
    public static final int REPAIR_AMOUNT = 25;
    public static final int ITEM_OUTPUTS = 27;
    public static final int FLUID_OUTPUTS = 9;
    public static final int GAS_OUTPUTS = 9;

    private MachineEnergyContainer<TileEntityVeinDrill> energyContainer;
    private InputInventorySlot repairSlot;
    private mekanism.api.inventory.IInventorySlot veinCoreSlot;
    private EnergyInventorySlot energySlot;
    private ExpandableOutputSlot[] itemOutputSlots;
    private ExpandableOutputSlot[] fluidOutputSlots;
    private ExpandableOutputSlot[] gasOutputSlots;

    private long durability = BASE_DURABILITY;
    private VeinData vein;
    private boolean autoEject;
    private boolean autoPull;
    /** 自动修复:低于阈值(50%)时自动消耗修复品 */
    private boolean autoRepair = true;
    private int operatingTicks;

    //独立升级:钻头等级(1~9,每次产出 ×2)
    private int drillUpgradeLevel = 1;
    /** 无限钻头:耐久不再消耗 */
    private boolean infiniteDrill;

    //客户端同步视图(容器打开期间实时同步到客户端 GUI)
    public long viewMassRemaining;
    public long viewMassCapacity;
    public int viewVeinSizeId = -1;
    public long viewDurability = -1;
    /** 升级槽内科技锭计数(客户端显示用) */
    public int viewUpgradeCount;

    //升级槽(科技锭:64 个 N 阶锭 -> 等级+1)
    private BasicInventorySlot upgradeSlot;

    public TileEntityVeinDrill(BlockPos pos, BlockState state) {
        this(com.yongaishide.chaosworld.mekanism.vein.VeinDrillMachines.VEIN_DRILL, pos, state);
    }

    public TileEntityVeinDrill(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        List<IInventorySlot> inputs = new ArrayList<>(List.of(repairSlot, veinCoreSlot, upgradeSlot));
        List<IInventorySlot> outputs = new ArrayList<>(ITEM_OUTPUTS + FLUID_OUTPUTS + GAS_OUTPUTS);
        for (int i = 0; i < ITEM_OUTPUTS; i++) {
            outputs.add(itemOutputSlots[i]);
        }
        for (int i = 0; i < FLUID_OUTPUTS; i++) {
            outputs.add(fluidOutputSlots[i]);
        }
        for (int i = 0; i < GAS_OUTPUTS; i++) {
            outputs.add(gasOutputSlots[i]);
        }
        configComponent.setupItemIOConfig(inputs, outputs, energySlot, false);
        //能量传输类型:注册槽位信息(仅输入),否则 isCapabilityDisabled 恒为 true,能量无法进入
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);
        ejectorComponent = new TileComponentEjector(this);
        //能量仅输入,不挂 eject;物品由侧配置输出
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        //固定面:能量从左侧/右侧进入
        EnergyContainerHelper builder = EnergyContainerHelper.forSide(facingSupplier);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener),
              mekanism.api.RelativeSide.LEFT, mekanism.api.RelativeSide.RIGHT);
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        //子类字段初始化发生在 super() 之后,而 super 构造器会回调此方法,因此数组必须在此惰性创建
        if (itemOutputSlots == null) {
            itemOutputSlots = new ExpandableOutputSlot[ITEM_OUTPUTS];
            fluidOutputSlots = new ExpandableOutputSlot[FLUID_OUTPUTS];
            gasOutputSlots = new ExpandableOutputSlot[GAS_OUTPUTS];
        }
        //固定面:输入(核心/科技锭/修复)从正上面+背面;三槽同排 x=7/27/47,间距 20 对齐
        InventorySlotHelper builder = InventorySlotHelper.forSide(facingSupplier);
        repairSlot = builder.addSlot(InputInventorySlot.at(TileEntityVeinDrill::isRepairItem, listener, 47, 92),
              mekanism.api.RelativeSide.TOP, mekanism.api.RelativeSide.BACK);
        veinCoreSlot = builder.addSlot(BasicInventorySlot.at(
              stack -> stack.getItem() instanceof com.yongaishide.chaosworld.mekanism.vein.VeinCoreItem,
              listener, 7, 92, 1),
              mekanism.api.RelativeSide.TOP, mekanism.api.RelativeSide.BACK);
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 152, 20));
        //升级槽:科技锭 input(只收 1~9 阶锭);主界面三槽同排居中(x=27,与核心 7/修复 47 对齐)
        upgradeSlot = builder.addSlot(BasicInventorySlot.at(
              stack -> isTechIngot(stack), listener, 27, 92, 64),
              mekanism.api.RelativeSide.TOP, mekanism.api.RelativeSide.BACK);
        upgradeSlot.setSlotType(mekanism.common.inventory.container.slot.ContainerSlotType.INPUT);
        //输出槽:物品 27(9x3)、流体 9、气体 9 —— 全部从后侧输出
        //输出槽:可扩容(单格堆叠 64 × 2^(等级-1)),全部从后侧输出
        java.util.function.IntSupplier levelSupplier = () -> drillUpgradeLevel;
        for (int i = 0; i < ITEM_OUTPUTS; i++) {
            int col = i % 9;
            int row = i / 9;
            itemOutputSlots[i] = builder.addSlot(ExpandableOutputSlot.at(64, levelSupplier, listener, 8 + col * 18, 114 + row * 18),
                  mekanism.api.RelativeSide.BACK);
        }
        for (int i = 0; i < FLUID_OUTPUTS; i++) {
            fluidOutputSlots[i] = builder.addSlot(ExpandableOutputSlot.at(64, levelSupplier, listener, 8 + (i % 9) * 18, 168),
                  mekanism.api.RelativeSide.BACK);
        }
        for (int i = 0; i < GAS_OUTPUTS; i++) {
            gasOutputSlots[i] = builder.addSlot(ExpandableOutputSlot.at(64, levelSupplier, listener, 8 + (i % 9) * 18, 186),
                  mekanism.api.RelativeSide.BACK);
        }
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        super.onUpdateServer();
        if (getLevel() == null || getLevel().isClientSide()) {
            return false;
        }
        //能量槽:能量物品(充电电池等)自动充入能量容器
        energySlot.fillContainerOrConvert();
        //升级槽:64 个 N 阶科技锭 -> 钻头等级+1(9 级时九阶锭 -> 无限钻头)
        checkUpgradeSlot();
        syncVeinFromCore();
        //自动维护:开关开启且耐久低于阈值(50%)时批量修复才消耗修复品
        if (autoRepair && !infiniteDrill && !repairSlot.isEmpty()
              && durability < Math.max(1, getMaxDurability() / 2)) {
            ItemStack used = repairSlot.getStack();
            int amount = repairAmount(used);
            repairSlot.shrinkStack(1, Action.EXECUTE);
            durability = Math.min(getMaxDurability(), durability + amount);
            setChanged();
        }
        //自动拉取:开启时从后侧边界块外侧容器拉取铁锭入修复槽
        if (autoPull) {
            autoPullIron();
        }
        if (canMine()) {
            //运行功耗:每 tick 256
            getEnergyContainer().extract(ENERGY_BASE, Action.EXECUTE, AutomationType.INTERNAL);
            operatingTicks++;
            if (operatingTicks >= ticksRequired) {
                operatingTicks = 0;
                mineOnce();
            }
        } else {
            operatingTicks = 0;
        }
        //锚点升级:加载所在区块
        if (getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            chunkLoaderComponent.tickServer();
        }
        //自动弹出:物品槽 -> 背侧边界块外侧的目标容器(多块结构,ejector 组件探测主块相邻无效)
        if (isAutoEject()) {
            autoEjectItems();
        }
        return true;
    }

    /**
     * 自动弹出(多接口):优先顶部容器(机器顶面 y=1 上方的容器),再尝试背侧容器。
     */
    private void autoEjectItems() {
        if (getLevel() == null || getLevel().isClientSide()) {
            return;
        }
        net.minecraft.core.Direction opposite = getOppositeDirection();
        //顶部接口:主块上方边界块(0,1,0)的正上方容器
        net.minecraft.core.BlockPos topTarget = getBlockPos().above().above();
        net.neoforged.neoforge.items.IItemHandler top = getLevel().getCapability(
              mekanism.common.capabilities.Capabilities.ITEM.block(), topTarget, net.minecraft.core.Direction.DOWN);
        if (top != null) {
            if (ejectToHandler(top)) {
                return;
            }
        }
        //背侧接口:主块上方背侧边界块的外侧
        net.minecraft.core.BlockPos ejectPos = getBlockPos().above().relative(opposite);
        net.minecraft.core.BlockPos backTarget = ejectPos.relative(opposite);
        net.neoforged.neoforge.items.IItemHandler back = getLevel().getCapability(
              mekanism.common.capabilities.Capabilities.ITEM.block(), backTarget, opposite);
        if (back != null) {
            ejectToHandler(back);
        }
    }

    /** @return 是否成功弹出过且无剩余溢出 */
    private boolean ejectToHandler(net.neoforged.neoforge.items.IItemHandler target) {
        boolean movedAny = false;
        for (ExpandableOutputSlot slot : itemOutputSlots) {
            if (slot.isEmpty()) {
                continue;
            }
            net.minecraft.world.item.ItemStack stack = slot.getStack();
            net.minecraft.world.item.ItemStack remaining = insertToHandler(target, stack.copy());
            int moved = stack.getCount() - remaining.getCount();
            if (moved > 0) {
                slot.setStack(remaining);
                movedAny = true;
            }
            if (remaining.getCount() > 0 && remaining.getCount() < stack.getCount()) {
                break;
            }
            if (remaining.getCount() == stack.getCount()) {
                break;
            }
        }
        return movedAny;
    }

    private static net.minecraft.world.item.ItemStack insertToHandler(net.neoforged.neoforge.items.IItemHandler handler,
          net.minecraft.world.item.ItemStack stack) {
        net.minecraft.world.item.ItemStack remaining = stack.copy();
        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            if (handler.isItemValid(i, remaining)) {
                remaining = handler.insertItem(i, remaining, false);
            }
        }
        return remaining;
    }

    /**
     * 核心槽里放了虚拟矿脉核心则读取其矿脉;没核心则清空。
     * 核心在,但无矿脉或矿脉质量耗尽时,自动扫描新矿脉(写回核心)。
     */
    private void syncVeinFromCore() {
        if (veinCoreSlot.isEmpty()) {
            vein = null;
            return;
        }
        VeinData coreVein = VeinData.VeinNbt.read(veinCoreSlot.getStack());
        if (coreVein != null && !coreVein.isEmpty()) {
            vein = coreVein;
        } else {
            //没有绑定矿脉或已采空 -> 自动扫描新矿脉
            vein = generateVein();
            VeinData.VeinNbt.write(veinCoreSlot.getStack(), vein);
            setChanged();
        }
    }

    private boolean canMine() {
        return !veinCoreSlot.isEmpty() && vein != null && !vein.isEmpty()
              && (infiniteDrill || durability >= 1)
              && getEnergyContainer().getEnergy() >= ENERGY_BASE;
    }

    private long energyPerMine() {
        return ENERGY_BASE;
    }

    /** 单次挖掘所需能量(GUI 能量不足警告使用,已变为每 tick 成本) */
    public long getEnergyPerMineForWarning() {
        return ENERGY_BASE;
    }

    /** 当前每 tick 平均能量消耗(单位 mFE,能量信息书签 tab 使用) */
    public long getEnergyPerTickMilli() {
        return ENERGY_BASE * 1000L;
    }

    private void mineOnce() {
        if (vein == null || vein.isEmpty()) {
            return;
        }
        //钻头消耗(无限钻头不消耗):并行 N 个配方 -> 掉 N 耐久
        if (!infiniteDrill) {
            durability = Math.max(0, durability - operationsPerTick);
        }
        //抽取统一池产物:只取 最低钻头等级 <= 当前等级 的条目(高等级矿池包含低等级)
        //drillUpgradeLevel 语义为 1~9
        OreEntry entry = rollUnified(drillUpgradeLevel);
        if (entry != null) {
            //并行升级:每次挖掘执行 operationsPerTick 次(只影响并行,不影响单格堆叠容量)
            long totalAmount = (long) operationsPerTick * drillsPerTick();
            insertOutput(entry, totalAmount);
            vein = vein.consumeMass(entry.qualityCost() * totalAmount);
            if (!veinCoreSlot.isEmpty()) {
                VeinData.VeinNbt.write(veinCoreSlot.getStack(), vein);
            }
        }
        setChanged();
    }

    /** 修复品判定:数据包 repair_items 注册(空则默认铁系) */
    public static boolean isRepairItem(ItemStack stack) {
        java.util.Map<net.minecraft.resources.ResourceLocation, Integer> items =
              com.yongaishide.chaosworld.mekanism.vein.VeinDrillDataLoader.getRepairItems();
        if (!items.isEmpty()) {
            return items.containsKey(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        return stack.is(Items.IRON_INGOT) || stack.is(Items.IRON_NUGGET) || stack.is(Items.IRON_BLOCK);
    }

    /** 单件修复量:数据包 repair 值(缺省 25);无数据包时铁系默认 */
    public static int repairAmount(ItemStack stack) {
        java.util.Map<net.minecraft.resources.ResourceLocation, Integer> items =
              com.yongaishide.chaosworld.mekanism.vein.VeinDrillDataLoader.getRepairItems();
        if (!items.isEmpty()) {
            Integer amount = items.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
            return amount != null ? amount : 25;
        }
        if (stack.is(Items.IRON_NUGGET)) {
            return REPAIR_AMOUNT / 8;
        }
        if (stack.is(Items.IRON_BLOCK)) {
            return REPAIR_AMOUNT * 9;
        }
        return REPAIR_AMOUNT;
    }

    /**
     * 自动拉取:从后侧边界块外侧容器拉取修复品入修复槽(开启 autoPull 时调用)。
     */
    private void autoPullIron() {
        if (getLevel() == null || getLevel().isClientSide() || repairSlot.getStack().getCount() >= repairSlot.getLimit(repairSlot.getStack())) {
            return;
        }
        net.minecraft.core.Direction opposite = getOppositeDirection();
        net.minecraft.core.BlockPos sourcePos = getBlockPos().above().relative(opposite).relative(opposite);
        net.neoforged.neoforge.items.IItemHandler source = getLevel().getCapability(
              mekanism.common.capabilities.Capabilities.ITEM.block(), sourcePos, opposite);
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack inSlot = source.getStackInSlot(i);
            if (inSlot.isEmpty() || !isRepairItem(inSlot)) {
                continue;
            }
            ItemStack extracted = source.extractItem(i, 1, false);
            if (!extracted.isEmpty()) {
                ItemStack leftover = repairSlot.insertItem(extracted, Action.EXECUTE, AutomationType.INTERNAL);
                if (!leftover.isEmpty()) {
                    //放不回时塞回原容器(尽力处理)
                    source.insertItem(i, leftover, false);
                }
                setChanged();
                return;
            }
        }
    }

    /**
     * 统一池按权重抽取;只统计 minDrillLevel <= 当前钻头等级 的条目。
     */
    private OreEntry rollUnified(int drillLevel) {
        List<OreEntry> pool = VeinDrillDataLoader.getUnifiedPool();
        if (pool.isEmpty()) {
            return null;
        }
        long total = 0;
        for (OreEntry entry : pool) {
            if (entry.minDrillLevel() <= drillLevel) {
                total += entry.weight();
            }
        }
        if (total <= 0) {
            return null;
        }
        long roll = java.util.concurrent.ThreadLocalRandom.current().nextLong(total);
        long cumulative = 0;
        for (OreEntry entry : pool) {
            if (entry.minDrillLevel() <= drillLevel) {
                cumulative += entry.weight();
                if (roll < cumulative) {
                    return entry;
                }
            }
        }
        return null;
    }

    /**
     * 把 totalAmount 个产物放入输出槽:单格上限 = 槽容量(64 × 2^(等级-1)),
     * 超容量自动拆分填往后续槽;全部放不下则丢弃剩余。
     */
    private void insertOutput(OreEntry entry, long totalAmount) {
        VeinData.Material material = VeinData.fromEntry(entry, getLevel());
        ItemStack base = material.itemStack();
        if (base.isEmpty()) {
            base = VeinData.itemStackFor(entry, getLevel());
        }
        ExpandableOutputSlot[] slots = switch (entry.type()) {
            case ITEM -> itemOutputSlots;
            case FLUID -> fluidOutputSlots;
            case GAS -> gasOutputSlots;
        };
        long remaining = Math.max(1, totalAmount);
        while (remaining > 0) {
            int limit = 0;
            for (ExpandableOutputSlot slot : slots) {
                int slotLimit = slot.getLimit(base);
                if (slotLimit > limit) {
                    limit = slotLimit;
                }
            }
            int batch = (int) Math.min(remaining, limit);
            if (batch <= 0) {
                break;
            }
            ItemStack stack = base.copy();
            stack.setCount(batch);
            boolean placed = false;
            for (ExpandableOutputSlot slot : slots) {
                boolean fits = slot.isEmpty()
                      || slot.insertItem(stack, Action.SIMULATE, AutomationType.INTERNAL).getCount() == 0;
                if (fits) {
                    slot.insertItem(stack, Action.EXECUTE, AutomationType.INTERNAL);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                //全部槽放不下,丢弃剩余防止爆仓
                return;
            }
            remaining -= batch;
        }
    }

    //===== 供 GUI 使用 =====
    public MachineEnergyContainer<TileEntityVeinDrill> getEnergyContainer() {
        return energyContainer;
    }

    public long getDurability() {
        return durability;
    }

    public VeinData getVein() {
        return vein;
    }

    public boolean isAutoEject() {
        //以侧配置的 ejecting 标志为准(单一数据源,可通过侧配置窗口/配置卡/数据卡修改)
        mekanism.common.tile.component.config.ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        return itemConfig != null && itemConfig.isEjecting();
    }

    public boolean isAutoPull() {
        return autoPull;
    }

    public void toggleAutoEject() {
        mekanism.common.tile.component.config.ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null) {
            itemConfig.setEjecting(!itemConfig.isEjecting());
            markForSave();
        }
        setChanged();
    }

    public void toggleAutoPull() {
        autoPull = !autoPull;
        setChanged();
    }

    public boolean isAutoRepair() {
        return autoRepair;
    }

    public void toggleAutoRepair() {
        autoRepair = !autoRepair;
        setChanged();
    }

    public void scanVein() {
        //当前矿脉还有质量时不允许扫描新矿脉
        if (veinCoreSlot.isEmpty() || (vein != null && !vein.isEmpty())) {
            return;
        }
        vein = generateVein();
        VeinData.VeinNbt.write(veinCoreSlot.getStack(), vein);
        setChanged();
    }

    /**
     * 容器打开期间实时同步 GUI 数据(信息屏/耐久)。
     * 通过 Syncable 值将 server 端文本变化推送到客户端。
     */
    @Override
    public void addContainerTrackers(mekanism.common.inventory.container.MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(new mekanism.common.inventory.container.sync.SyncableLong() {
            @Override
            public long get() {
                return vein == null ? 0 : vein.massRemaining();
            }

            @Override
            public void set(long value) {
                viewMassRemaining = value;
            }
        });
        container.track(new mekanism.common.inventory.container.sync.SyncableLong() {
            @Override
            public long get() {
                return vein == null ? 0 : vein.massCapacity();
            }

            @Override
            public void set(long value) {
                viewMassCapacity = value;
            }
        });
        container.track(new mekanism.common.inventory.container.sync.SyncableInt() {
            @Override
            public int get() {
                return vein == null ? -1 : vein.size().getId();
            }

            @Override
            public void set(int value) {
                viewVeinSizeId = value;
            }
        });
        container.track(new mekanism.common.inventory.container.sync.SyncableLong() {
            @Override
            public long get() {
                return durability;
            }

            @Override
            public void set(long value) {
                viewDurability = value;
            }
        });
        //GUI 弹出开关状态:以侧配置 ejecting 标志同步(客户端 set 写回 config,保证显示=服务端状态)
        container.track(new mekanism.common.inventory.container.sync.SyncableBoolean() {
            @Override
            public boolean get() {
                return isAutoEject();
            }

            @Override
            public void set(boolean value) {
                mekanism.common.tile.component.config.ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
                if (itemConfig != null) {
                    itemConfig.setEjecting(value);
                }
            }
        });
        //专属升级等级同步(客户端升级窗口显示服务端真实等级)
        container.track(new mekanism.common.inventory.container.sync.SyncableInt() {
            @Override
            public int get() {
                return drillUpgradeLevel;
            }

            @Override
            public void set(int value) {
                drillUpgradeLevel = value;
            }
        });
        container.track(new mekanism.common.inventory.container.sync.SyncableBoolean() {
            @Override
            public boolean get() {
                return infiniteDrill;
            }

            @Override
            public void set(boolean value) {
                infiniteDrill = value;
            }
        });
        container.track(new mekanism.common.inventory.container.sync.SyncableBoolean() {
            @Override
            public boolean get() {
                return autoRepair;
            }

            @Override
            public void set(boolean value) {
                autoRepair = value;
            }
        });
    }

    /** 科技锭等级(1~9),非科技锭返回 0 */
    private static int techIngotTier(net.minecraft.world.item.ItemStack stack) {
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        int base = "ingot_tech_".length();
        if (path.startsWith("ingot_tech_") && path.length() == base + 1) {
            char c = path.charAt(base);
            return c >= '1' && c <= '9' ? (c - '0') : 0;
        }
        return 0;
    }

    public static boolean isTechIngot(net.minecraft.world.item.ItemStack stack) {
        return techIngotTier(stack) > 0;
    }

    /**
     * 升级槽检查:满 64 个且阶数匹配当前等级需求才消耗升级;
     * 阶数不匹配/数量不足时**不消耗任何材料**。
     * 规则:1~8 级用 当前等级N 阶锭(N->N+1);9 级用九阶锭 -> 无限钻头。
     */
    private void checkUpgradeSlot() {
        if (upgradeSlot == null || upgradeSlot.isEmpty()) {
            return;
        }
        ItemStack stack = upgradeSlot.getStack();
        int tier = techIngotTier(stack);
        if (tier <= 0 || stack.getCount() < 64) {
            //非科技锭或数量不足:不动材料
            return;
        }
        if (infiniteDrill) {
            return;
        }
        boolean canUpgrade;
        if (drillUpgradeLevel >= 9) {
            //9 级只有九阶锭可转无限钻头
            canUpgrade = tier >= 9;
        } else {
            //当前等级 N 需要 N 阶锭
            canUpgrade = tier == drillUpgradeLevel;
        }
        if (!canUpgrade) {
            //阶数不匹配:不消耗
            return;
        }
        //满足条件,消耗 64(多出的保留)
        if (stack.getCount() > 64) {
            stack.setCount(stack.getCount() - 64);
        } else {
            upgradeSlot.setStack(ItemStack.EMPTY);
        }
        if (drillUpgradeLevel >= 9) {
            infiniteDrill = true;
            durability = getMaxDurability();
        } else {
            drillUpgradeLevel++;
            durability = Math.min(getMaxDurability(), durability + getMaxDurability() / 2);
        }
        setChanged();
    }

    /**
     * 生成新矿脉(按维度随机大小等级)。
     */
    private VeinData generateVein() {
        return VeinData.generate(getLevel());
    }

    /**
     * 耐久上限 = 1000 × 2^(钻头等级-1),钻头等级 1~9。
     */
    public long getMaxDurability() {
        return BASE_DURABILITY * (1L << Math.max(0, drillUpgradeLevel - 1));
    }

    /** 钻头等级(1~9)。 */
    public int getDrillLevel() {
        return drillUpgradeLevel;
    }

    /** 无限钻头(耐久不消耗)。 */
    public boolean isInfiniteDrill() {
        return infiniteDrill;
    }

    /**
     * 单次产出数量:钻头等级 1/2/3/4 -> 1/4/16/64。
     */
    private int drillsPerTick() {
        return pow4(Math.max(0, drillUpgradeLevel - 1));
    }

    /** 4^n,上限防溢出 */
    public static int pow4Public(int n) {
        int result = 1;
        for (int i = 0; i < n; i++) {
            result *= 4;
            if (result >= Integer.MAX_VALUE / 4) {
                return Integer.MAX_VALUE;
            }
        }
        return result;
    }

    private static int pow4(int n) {
        return pow4Public(n);
    }

    /**
     * 独立升级:钻头等级+1(上限 9)。
     */
    public void upgradeDrill() {
        if (drillUpgradeLevel < 9) {
            drillUpgradeLevel++;
            durability = Math.min(getMaxDurability(), durability + getMaxDurability() / 2);
            setChanged();
        }
    }

    /**
     * 手动修复:消耗 1 件修复品,按类型恢复耐久。
     */
    public void repair() {
        if (!repairSlot.isEmpty()) {
            int amount = repairAmount(repairSlot.getStack());
            repairSlot.shrinkStack(1, Action.EXECUTE);
            durability = Math.min(getMaxDurability(), durability + amount);
            setChanged();
        }
    }

    public int getOperatingTicks() {
        return operatingTicks;
    }

    /** 当前挖掘周期(受速度升级影响) */
    public int getTicksRequired() {
        return ticksRequired;
    }

    @Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("durability", durability);
        tag.putInt("operatingTicks", operatingTicks);

        tag.putBoolean("autoEject", autoEject);
        tag.putBoolean("autoPull", autoPull);
        tag.putBoolean("autoRepair", autoRepair);
        tag.putInt("drillUpgradeLevel", drillUpgradeLevel);
        tag.putBoolean("infiniteDrill", infiniteDrill);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        durability = tag.getLong("durability");
        operatingTicks = tag.getInt("operatingTicks");

        autoEject = tag.getBoolean("autoEject");
        if (autoEject) {
            //旧 NBT 兼容:老版本的 autoEject 只存于 tile NBT,迁移到侧配置 ejecting 标志
            mekanism.common.tile.component.config.ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
            if (itemConfig != null) {
                itemConfig.setEjecting(true);
            }
        }
        autoPull = tag.getBoolean("autoPull");
        autoRepair = tag.contains("autoRepair") ? tag.getBoolean("autoRepair") : true;
        drillUpgradeLevel = tag.getInt("drillUpgradeLevel");
        if (drillUpgradeLevel < 1) {
            //旧档兼容:0 起步时代的数据抬到 1
            drillUpgradeLevel = 1;
        }
        infiniteDrill = tag.getBoolean("infiniteDrill");
        vein = null;
    }

}
