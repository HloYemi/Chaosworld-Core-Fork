package com.yongaishide.chaosworld.mekanism.vein;

import com.yongaishide.chaosworld.ChaosWorld;
import com.yongaishide.chaosworld.mekanism.MekanismMachines;
import com.yongaishide.chaosworld.mekanism.vein.tile.TileEntityVeinDrill;
import mekanism.api.Upgrade;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
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
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 虚脉钻探机(Vein Drill)注册中心。
 */
public final class VeinDrillMachines {

    private VeinDrillMachines() {
    }

    //独立物品注册器(虚拟矿脉核心)
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, ChaosWorld.MODID);

    public static final DeferredHolder<Item, VeinCoreItem> VEIN_CORE =
          ITEMS.register("vein_core", () -> new VeinCoreItem(new Item.Properties()));

    /**
     * 机器默认侧配置(固定面,无需用户配置):物品 正上面=输入、后侧=输出;能量 左/右=输入。
     */
    private static final mekanism.common.attachments.component.AttachedSideConfig VEIN_DRILL_DEFAULT_SIDE_CONFIG =
          new mekanism.common.attachments.component.AttachedSideConfig(net.minecraft.Util.make(() -> {
              java.util.Map<mekanism.common.lib.transmitter.TransmissionType, mekanism.common.attachments.component.AttachedSideConfig.LightConfigInfo> map =
                    new java.util.EnumMap<>(mekanism.common.lib.transmitter.TransmissionType.class);
              java.util.Map<mekanism.api.RelativeSide, mekanism.common.tile.component.config.DataType> itemSides =
                    new java.util.EnumMap<>(mekanism.api.RelativeSide.class);
              itemSides.put(mekanism.api.RelativeSide.TOP, mekanism.common.tile.component.config.DataType.INPUT);
              itemSides.put(mekanism.api.RelativeSide.BACK, mekanism.common.tile.component.config.DataType.OUTPUT);
              map.put(mekanism.common.lib.transmitter.TransmissionType.ITEM, new mekanism.common.attachments.component.AttachedSideConfig.LightConfigInfo(itemSides, false));
              java.util.Map<mekanism.api.RelativeSide, mekanism.common.tile.component.config.DataType> energySides =
                    new java.util.EnumMap<>(mekanism.api.RelativeSide.class);
              energySides.put(mekanism.api.RelativeSide.LEFT, mekanism.common.tile.component.config.DataType.INPUT);
              energySides.put(mekanism.api.RelativeSide.RIGHT, mekanism.common.tile.component.config.DataType.INPUT);
              map.put(mekanism.common.lib.transmitter.TransmissionType.ENERGY, new mekanism.common.attachments.component.AttachedSideConfig.LightConfigInfo(energySides, false));
              return map;
          }));

    //===== 矿机方块 / 类型 / 方块实体 =====
    public static final Machine<TileEntityVeinDrill> VEIN_DRILL_TYPE = MachineBuilder
          .createMachine(VeinDrillMachines::veinDrillTile, new VeinDrillLang())
          .withGui(VeinDrillMachines::veinDrillContainer)
          .withSound(MekanismSounds.CHEMICAL_CRYSTALLIZER)
          .withEnergyConfig(() -> 256L, () -> 80_000L)
          .with(AttributeUpgradeSupport.create(com.jerry.mekextras.api.ExtraUpgrade.STACK,
                Upgrade.SPEED, Upgrade.ENERGY, Upgrade.ANCHOR))
          .withSideConfig(TransmissionType.ITEM, TransmissionType.ENERGY)
          .withCustomShape(BlockShapes.DIGITAL_MINER)
          .withBounding(new mekanism.common.block.attribute.AttributeHasBounding.HandleBoundingBlock() {
              @Override
              public <DATA> boolean handle(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                    net.minecraft.world.level.block.state.BlockState state, DATA data,
                    mekanism.common.block.attribute.AttributeHasBounding.TriBooleanFunction<net.minecraft.world.level.Level, net.minecraft.core.BlockPos, DATA> accept) {
                  //与数字采矿机一致:master 中心格,周边 3x2x3(不含原点)为边界块
                  net.minecraft.core.BlockPos.MutableBlockPos mutable = new net.minecraft.core.BlockPos.MutableBlockPos();
                  for (int dx = -1; dx <= 1; dx++) {
                      for (int dy = 0; dy <= 1; dy++) {
                          for (int dz = -1; dz <= 1; dz++) {
                              if (dx == 0 && dy == 0 && dz == 0) {
                                  continue;
                              }
                              mutable.set(pos).move(dx, dy, dz);
                              if (!accept.accept(level, mutable, data)) {
                                  return false;
                              }
                          }
                      }
                  }
                  return true;
              }
          })
          .withComputerSupport("digitalMiner")
          .build();

    @SuppressWarnings("unchecked")
    public static final BlockRegistryObject<BlockTileModel<TileEntityVeinDrill, Machine<TileEntityVeinDrill>>,
          ItemBlockTooltip<BlockTileModel<TileEntityVeinDrill, Machine<TileEntityVeinDrill>>>> VEIN_DRILL =
          MekanismMachines.BLOCKS.register("vein_drill",
                () -> new BlockTileModel<>(VEIN_DRILL_TYPE, properties -> properties.mapColor(MapColor.METAL)),
                (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                      .component(mekanism.common.registries.MekanismDataComponents.EJECTOR, mekanism.common.attachments.component.AttachedEjector.DEFAULT)
                      .component(mekanism.common.registries.MekanismDataComponents.SIDE_CONFIG, VEIN_DRILL_DEFAULT_SIDE_CONFIG)))
                .forItemHolder(item -> item.addAttachedContainerCapabilities(mekanism.common.attachments.containers.ContainerType.ITEM,
                      () -> mekanism.common.attachments.containers.item.ItemSlotsBuilder.builder()
                            .addInput(TileEntityVeinDrill::isRepairItem)
                            .addInput(stack -> stack.getItem() instanceof VeinCoreItem)
                            .addEnergy()
                            .addInput(TileEntityVeinDrill::isTechIngot)
                            .addOutput(TileEntityVeinDrill.ITEM_OUTPUTS + TileEntityVeinDrill.FLUID_OUTPUTS + TileEntityVeinDrill.GAS_OUTPUTS)
                            .build()));

    public static final TileEntityTypeRegistryObject<TileEntityVeinDrill> VEIN_DRILL_TILE =
          MekanismMachines.TILE_ENTITY_TYPES
                .mekBuilder(VEIN_DRILL, TileEntityVeinDrill::new)
                .clientTicker((level, pos, state, be) -> TileEntityMekanism.tickClient(level, pos, state, be))
                .serverTicker((level, pos, state, be) -> TileEntityMekanism.tickServer(level, pos, state, be))
                .build();

    public static final ContainerTypeRegistryObject<VeinDrillContainer> VEIN_DRILL_CONTAINER =
          MekanismMachines.CONTAINER_TYPES.register("vein_drill", TileEntityVeinDrill.class, VeinDrillContainer::new);

    /**
     * 触发静态初始化(注册矿机方块/方块实体/容器到 MekanismMachines 的共享注册器)。
     * 必须在 MekanismMachines.register(eventBus) 之前调用。
     */
    public static void init() {
        //JVM 保证静态块只执行一次
    }

    public static void register(IEventBus eventBus) {
        //BLOCKS/TILE_ENTITY_TYPES/CONTAINER_TYPES 已在 MekanismMachines.register 中注册(复用同一注册器);
        //这里只注册本模块的物品
        ITEMS.register(eventBus);
    }

    private static TileEntityTypeRegistryObject<TileEntityVeinDrill> veinDrillTile() {
        return VEIN_DRILL_TILE;
    }

    private static ContainerTypeRegistryObject<VeinDrillContainer> veinDrillContainer() {
        return VEIN_DRILL_CONTAINER;
    }

    static class VeinDrillLang implements mekanism.api.text.ILangEntry {

        @Override
        public String getTranslationKey() {
            return "description.chaosworld_core.vein_drill";
        }
    }
}
