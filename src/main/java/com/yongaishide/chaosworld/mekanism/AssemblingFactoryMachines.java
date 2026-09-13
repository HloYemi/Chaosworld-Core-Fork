package com.yongaishide.chaosworld.mekanism;

import com.jerry.mekextras.api.ExtraUpgrade;
import com.jerry.mekextras.api.tier.AdvancedTier;
import com.jerry.mekextras.common.block.attribute.ExtraAttributeTier;
import com.jerry.mekextras.common.block.attribute.ExtraAttributeUpgradeable;
import com.yongaishide.chaosworld.mekanism.item.CircuitAssemblingFactoryItem;
import com.yongaishide.chaosworld.mekanism.tile.TileEntityAssemblingFactory;
import mekanism.api.Upgrade;
import mekanism.api.tier.BaseTier;
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
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.registries.MekanismSounds;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * Assembling Factory: 8 tiers (Mekanism 4 + mekextras 4), all sharing the same tile/GUI/container.
 * <p>
 * Upgrade chain: basic -> advanced -> elite -> ultimate (Mekanism tier installers),
 * ultimate -> absolute -> supreme -> cosmic -> infinite (mekextras tier installers).
 * Block models all reuse the Pressurized Reaction Chamber (PRC) model.
 */
public final class AssemblingFactoryMachines {

    private AssemblingFactoryMachines() {
    }

    private static final int COUNT = 8;
    private static final String[] NAMES = {
          "basic_assembling_factory", "advanced_assembling_factory", "elite_assembling_factory", "ultimate_assembling_factory",
          "absolute_assembling_factory", "supreme_assembling_factory", "cosmic_assembling_factory", "infinite_assembling_factory"
    };
    private static final FactoryTier[] MEK_TIERS = {FactoryTier.BASIC, FactoryTier.ADVANCED, FactoryTier.ELITE, FactoryTier.ULTIMATE};
    private static final AdvancedTier[] EXTRA_TIERS = {AdvancedTier.ABSOLUTE, AdvancedTier.SUPREME, AdvancedTier.COSMIC, AdvancedTier.INFINITE};
    //每个等级的能量上限,与粉碎工厂 8 级一致:
    //MEK 4 级统一 20,000 J;mekextras 4 级 = 20,000 × 处理份数(11/13/15/17)
    private static final long[] FACTORY_STORAGES = {20_000L, 20_000L, 20_000L, 20_000L, 220_000L, 260_000L, 300_000L, 340_000L};

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final BlockRegistryObject[] FACTORY_BLOCKS = new BlockRegistryObject[COUNT];
    @SuppressWarnings("unchecked")
    public static final TileEntityTypeRegistryObject<TileEntityAssemblingFactory>[] FACTORY_TILES = new TileEntityTypeRegistryObject[COUNT];
    @SuppressWarnings("unchecked")
    public static final Machine<TileEntityAssemblingFactory>[] FACTORY_TYPES = new Machine[COUNT];

    /**
     * Triggers static initialization (registers all blocks/tiles/types). Called during mod construction.
     */
    public static void init() {
        // JVM guarantees the static block runs exactly once; this call just forces it before registration events.
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
                  .<TileEntityAssemblingFactory>mekBuilder(FACTORY_BLOCKS[index], TileEntityAssemblingFactory::new)
                  .clientTicker((net.minecraft.world.level.block.entity.BlockEntityTicker<TileEntityAssemblingFactory>) (level, pos, state, be) -> TileEntityMekanism.tickClient(level, pos, state, be))
                  .serverTicker((net.minecraft.world.level.block.entity.BlockEntityTicker<TileEntityAssemblingFactory>) (level, pos, state, be) -> TileEntityMekanism.tickServer(level, pos, state, be))
                  .build();
        }
    }

    private static void registerTypes() {
        for (int i = 0; i < COUNT; i++) {
            final int index = i;
            MachineBuilder<Machine<TileEntityAssemblingFactory>, TileEntityAssemblingFactory, ?> builder = MachineBuilder
                  .createMachine(() -> factoryTile(index), new MechanicalAssemblerLang(NAMES[index]))
                  .withGui(MekanismMachines::containerType)
                  .withSound(MekanismSounds.CHEMICAL_CRYSTALLIZER)
                  .withEnergyConfig(() -> 50L, () -> FACTORY_STORAGES[index])
                  .withSideConfig(TransmissionType.ITEM, TransmissionType.CHEMICAL, TransmissionType.FLUID, TransmissionType.ENERGY)
                  .withCustomShape(BlockShapes.PRESSURIZED_REACTION_CHAMBER);
            if (index < 4) {
                // MEK 4 级:默认升级集(速度/能量/消音)
                builder = builder.with(new AttributeTier<FactoryTier>(MEK_TIERS[index]));
                if (index < 3) {
                    builder = builder.with(new AttributeUpgradeable(() -> factoryBlock(index + 1)));
                } else {
                    // Ultimate tier: upgraded to Absolute by the mekextras installer (fromTier = null)
                    builder = builder.with(new ExtraAttributeUpgradeable(() -> factoryBlock(index + 1)));
                }
            } else {
                // mekextras 4 级:默认升级集 + 堆叠升级
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

    @Nullable
    private static BlockRegistryObject<?, ?> factoryBlock(int index) {
        return FACTORY_BLOCKS[index];
    }

    @Nullable
    private static TileEntityTypeRegistryObject<TileEntityAssemblingFactory> factoryTile(int index) {
        return FACTORY_TILES[index];
    }

    @Nullable
    private static Machine<TileEntityAssemblingFactory> factoryType(int index) {
        return FACTORY_TYPES[index];
    }
}
