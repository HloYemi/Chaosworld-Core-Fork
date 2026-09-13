package com.yongaishide.chaosworld.mekanism;

import com.jerry.mekextras.api.ExtraUpgrade;
import com.jerry.mekextras.api.tier.AdvancedTier;
import com.jerry.mekextras.common.block.attribute.ExtraAttributeTier;
import com.jerry.mekextras.common.block.attribute.ExtraAttributeUpgradeable;
import com.yongaishide.chaosworld.ChaosWorld;
import com.yongaishide.chaosworld.mekanism.inventory.container.DragonSoulForgeContainer;
import com.yongaishide.chaosworld.mekanism.inventory.container.MechanicalAssemblerContainer;
import com.yongaishide.chaosworld.mekanism.item.CircuitAssemblingFactoryItem;
import com.yongaishide.chaosworld.mekanism.recipe.AssemblingRecipe;
import com.yongaishide.chaosworld.mekanism.recipe.BasicDragonSoulForgingRecipe;
import com.yongaishide.chaosworld.mekanism.recipe.ChemicalInputCache;
import com.yongaishide.chaosworld.mekanism.recipe.ChaosRecipeType;
import com.yongaishide.chaosworld.mekanism.recipe.DragonSoulForgingRecipeSerializer;
import com.yongaishide.chaosworld.mekanism.tile.AssemblingFactoryTiers;
import com.yongaishide.chaosworld.mekanism.tile.TileEntityDragonSoulForge;
import com.yongaishide.chaosworld.mekanism.tile.TileEntityDragonSoulForgeFactory;
import com.yongaishide.chaosworld.mekanism.tile.TileEntityMechanicalAssembler;
import mekanism.api.Upgrade;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.block.prefab.BlockTile.BlockTileModel;
import mekanism.common.content.blocktype.BlockShapes;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.content.blocktype.Machine.MachineBuilder;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.registries.MekanismSounds;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * 龙魂锻炉(Dragon Soul Forge)注册中心:单机 + 8 级工厂(MEK 4 级 + mekextras 4 级)。
 * <p>
 * 方块贴图/模型与电路组装机一致(均为加压反应室模型);GUI 暂定沿用电路组装机界面。
 * 配方类型独立为 {@code chaosworld_core:dragon_soul_forging}。
 * <p>
 * 升级链:basic -> advanced -> elite -> ultimate (Mekanism 等级安装器),
 * ultimate -> absolute -> supreme -> cosmic -> infinite (mekextras 等级安装器)。
 */
public final class DragonSoulForgeMachines {

    private DragonSoulForgeMachines() {
    }

    //能量配置与电路组装机一致:消耗 50 J/t、上限 20,000 J(所有等级统一)
    private static final long ENERGY_PER_TICK = 50L;
    private static final long ENERGY_STORAGE = 20_000L;

    //===== 配方类型:chaosworld_core:dragon_soul_forging =====
    //与 ASSEMBLING 同构,但注册名不同:锻炉单独缓存/查询配方
    public static final String DRAGON_SOUL_FORGING_ID = "dragon_soul_forging";
    public static final ChaosRecipeType<RecipeInput, AssemblingRecipe, ChemicalInputCache<AssemblingRecipe>> DRAGON_SOUL_FORGING =
          new ChaosRecipeType<>(ChaosWorld.id(DRAGON_SOUL_FORGING_ID), ChemicalInputCache::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<?>> DRAGON_SOUL_FORGING_TYPE =
          MekanismMachines.RECIPE_TYPES.register(DRAGON_SOUL_FORGING_ID, () -> DRAGON_SOUL_FORGING);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> DRAGON_SOUL_FORGING_SERIALIZER =
          MekanismMachines.RECIPE_SERIALIZERS.register(DRAGON_SOUL_FORGING_ID, () -> DragonSoulForgingRecipeSerializer.INSTANCE);

    //===== 单机:方块类型(仿照 MECHANICAL_ASSEMBLER_TYPE) =====
    //锻炉无化学槽,不启用 CHEMICAL 侧配置(tile 中基类的化学配置注册会因此自动跳过)
    public static final Machine<TileEntityDragonSoulForge> DRAGON_SOUL_FORGE_TYPE = MachineBuilder
          .createMachine(DragonSoulForgeMachines::tileType, new MechanicalAssemblerLang("dragon_soul_forge"))
          .withGui(DragonSoulForgeMachines::containerType)
          .withSound(MekanismSounds.CHEMICAL_CRYSTALLIZER)
          .withEnergyConfig(() -> ENERGY_PER_TICK, () -> ENERGY_STORAGE)
          .withSideConfig(TransmissionType.ITEM, TransmissionType.FLUID, TransmissionType.ENERGY)
          .withCustomShape(BlockShapes.PRESSURIZED_REACTION_CHAMBER)
          .build();

    //===== 单机:方块注册 =====
    public static final BlockRegistryObject<BlockTileModel<TileEntityDragonSoulForge, Machine<TileEntityDragonSoulForge>>,
          ItemBlockTooltip<BlockTileModel<TileEntityDragonSoulForge, Machine<TileEntityDragonSoulForge>>>> DRAGON_SOUL_FORGE =
          MekanismMachines.BLOCKS.register("dragon_soul_forge",
                () -> new BlockTileModel<>(DRAGON_SOUL_FORGE_TYPE, properties -> properties.mapColor(MapColor.METAL)),
                (block, properties) -> new ItemBlockTooltip<>(block, true, properties));

    //===== 单机:方块实体注册 =====
    public static final TileEntityTypeRegistryObject<TileEntityDragonSoulForge> DRAGON_SOUL_FORGE_TILE = MekanismMachines.TILE_ENTITY_TYPES
          .mekBuilder(DRAGON_SOUL_FORGE, TileEntityDragonSoulForge::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .build();

    //===== 单机:容器注册 =====
    //容器注册的 tile 类仅用于从网络包解码方块实体,锻炉类是其子类,可安全强转
    public static final ContainerTypeRegistryObject<DragonSoulForgeContainer> DRAGON_SOUL_FORGE_CONTAINER =
          MekanismMachines.CONTAINER_TYPES.register("dragon_soul_forge", TileEntityMechanicalAssembler.class,
                DragonSoulForgeMachines::createDragonSoulForgeContainer);

    //===== 工厂:8 级 =====
    private static final int COUNT = 8;
    private static final String[] NAMES = {
          "basic_dragon_soul_forge", "advanced_dragon_soul_forge", "elite_dragon_soul_forge", "ultimate_dragon_soul_forge",
          "absolute_dragon_soul_forge", "supreme_dragon_soul_forge", "cosmic_dragon_soul_forge", "infinite_dragon_soul_forge"
    };
    private static final FactoryTier[] MEK_TIERS = {FactoryTier.BASIC, FactoryTier.ADVANCED, FactoryTier.ELITE, FactoryTier.ULTIMATE};
    private static final AdvancedTier[] EXTRA_TIERS = {AdvancedTier.ABSOLUTE, AdvancedTier.SUPREME, AdvancedTier.COSMIC, AdvancedTier.INFINITE};

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final BlockRegistryObject[] FACTORY_BLOCKS = new BlockRegistryObject[COUNT];
    @SuppressWarnings("unchecked")
    public static final TileEntityTypeRegistryObject<TileEntityDragonSoulForgeFactory>[] FACTORY_TILES = new TileEntityTypeRegistryObject[COUNT];
    @SuppressWarnings("unchecked")
    public static final Machine<TileEntityDragonSoulForgeFactory>[] FACTORY_TYPES = new Machine[COUNT];

    /**
     * Triggers static initialization (registers all blocks/tiles/types). Called during mod construction.
     */
    public static void init() {
        // JVM guarantees the static block runs exactly once; this call just forces it before registration events.
    }

    /**
     * 给机器物品注册默认容器 creator(物品形式内部内容持久化 + tooltip 读取)。
     * 不注册时,采集机器得到的带附件数据的物品在悬停时会抛
     * "No known containers for item ..."。需在 mod 构造期调用。
     */
    public static void registerItemContainers(net.neoforged.bus.api.IEventBus modEventBus) {
        registerItemContainers(modEventBus, DRAGON_SOUL_FORGE.asItem(), 9, 1, 64_000L);
        for (int i = 0; i < COUNT; i++) {
            int threads = processesOf((net.minecraft.world.level.block.Block) FACTORY_BLOCKS[i].get());
            registerItemContainers(modEventBus, FACTORY_BLOCKS[i].asItem(), threads, threads, 64_000L * threads);
        }
    }

    private static int processesOf(net.minecraft.world.level.block.Block block) {
        return AssemblingFactoryTiers.getProcesses(block.defaultBlockState());
    }

    private static void registerItemContainers(net.neoforged.bus.api.IEventBus modEventBus,
          net.minecraft.world.item.Item item, int inputSlots, int outputSlots, long fluidCapacity) {
        mekanism.common.attachments.containers.ContainerType.FLUID.addDefaultCreators(modEventBus, item,
              () -> mekanism.common.attachments.containers.fluid.FluidTanksBuilder.builder()
                    .addBasic((int) fluidCapacity)
                    .build());
        mekanism.common.attachments.containers.ContainerType.ITEM.addDefaultCreators(modEventBus, item,
              () -> mekanism.common.attachments.containers.item.ItemSlotsBuilder.builder()
                    .addInput(inputSlots)
                    .addOutput(outputSlots)
                    .addBasic(1)
                    .addEnergy()
                    .build());
        mekanism.common.attachments.containers.ContainerType.ENERGY.addDefaultCreators(modEventBus, item,
              () -> mekanism.common.attachments.containers.energy.EnergyContainersBuilder.builder()
                    .addBasic(() -> ENERGY_STORAGE, () -> ENERGY_STORAGE)
                    .build());
    }

    static {
        registerBlocks();
        registerTiles();
        registerTypes();
    }

    private static void registerBlocks() {
        for (int i = 0; i < COUNT; i++) {
            final int index = i;
            FACTORY_BLOCKS[i] = MekanismMachines.BLOCKS.register(NAMES[i],
                  () -> new BlockTileModel<>(factoryType(index), properties -> properties.mapColor(MapColor.METAL)),
                  (block, properties) -> new CircuitAssemblingFactoryItem<>(block, true, properties));
        }
    }

    private static void registerTiles() {
        for (int i = 0; i < COUNT; i++) {
            final int index = i;
            FACTORY_TILES[i] = MekanismMachines.TILE_ENTITY_TYPES
                  .<TileEntityDragonSoulForgeFactory>mekBuilder(FACTORY_BLOCKS[index], TileEntityDragonSoulForgeFactory::new)
                  .clientTicker((net.minecraft.world.level.block.entity.BlockEntityTicker<TileEntityDragonSoulForgeFactory>) (level, pos, state, be) -> TileEntityMekanism.tickClient(level, pos, state, be))
                  .serverTicker((net.minecraft.world.level.block.entity.BlockEntityTicker<TileEntityDragonSoulForgeFactory>) (level, pos, state, be) -> TileEntityMekanism.tickServer(level, pos, state, be))
                  .build();
        }
    }

    private static void registerTypes() {
        for (int i = 0; i < COUNT; i++) {
            final int index = i;
            MachineBuilder<Machine<TileEntityDragonSoulForgeFactory>, TileEntityDragonSoulForgeFactory, ?> builder = MachineBuilder
                  .createMachine(() -> factoryTile(index), new MechanicalAssemblerLang(NAMES[index]))
                  .withGui(DragonSoulForgeMachines::containerType)
                  .withSound(MekanismSounds.CHEMICAL_CRYSTALLIZER)
                  .withEnergyConfig(() -> ENERGY_PER_TICK, () -> ENERGY_STORAGE)
                  .withSideConfig(TransmissionType.ITEM, TransmissionType.FLUID, TransmissionType.ENERGY)
                  .withCustomShape(BlockShapes.PRESSURIZED_REACTION_CHAMBER);
            if (index < 4) {
                builder = builder.with(new AttributeTier<FactoryTier>(MEK_TIERS[index]));
                if (index < 3) {
                    builder = builder.with(new AttributeUpgradeable(() -> factoryBlock(index + 1)));
                } else {
                    builder = builder.with(new ExtraAttributeUpgradeable(() -> factoryBlock(index + 1)));
                }
            } else {
                builder = builder.without(AttributeUpgradeSupport.class)
                      .with(AttributeUpgradeSupport.create(Upgrade.SPEED, Upgrade.ENERGY, Upgrade.MUFFLING, ExtraUpgrade.STACK))
                      .with(new ExtraAttributeTier<AdvancedTier>(EXTRA_TIERS[index - 4]));
                if (index < COUNT - 1) {
                    builder = builder.with(new ExtraAttributeUpgradeable(() -> factoryBlock(index + 1)));
                }
            }
            FACTORY_TYPES[i] = builder.build();
        }
    }

    public static String getName(int index) {
        return NAMES[index];
    }

    public static int getCount() {
        return COUNT;
    }

    //静态字段存在循环引用(TYPE -> TILE -> BLOCK -> TYPE),用访问器方法绕过 javac 的前向引用检查
    private static TileEntityTypeRegistryObject<TileEntityDragonSoulForge> tileType() {
        return DRAGON_SOUL_FORGE_TILE;
    }

    public static ContainerTypeRegistryObject<DragonSoulForgeContainer> containerType() {
        return DRAGON_SOUL_FORGE_CONTAINER;
    }

    private static DragonSoulForgeContainer createDragonSoulForgeContainer(int windowId,
          net.minecraft.world.entity.player.Inventory inv, TileEntityMechanicalAssembler tile) {
        return new DragonSoulForgeContainer(containerType(), windowId, inv, tile);
    }

    @Nullable
    private static BlockRegistryObject<?, ?> factoryBlock(int index) {
        return FACTORY_BLOCKS[index];
    }

    @Nullable
    private static TileEntityTypeRegistryObject<TileEntityDragonSoulForgeFactory> factoryTile(int index) {
        return FACTORY_TILES[index];
    }

    @Nullable
    private static Machine<TileEntityDragonSoulForgeFactory> factoryType(int index) {
        return FACTORY_TYPES[index];
    }
}
