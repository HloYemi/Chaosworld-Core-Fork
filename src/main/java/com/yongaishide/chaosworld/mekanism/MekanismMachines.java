package com.yongaishide.chaosworld.mekanism;

import com.yongaishide.chaosworld.ChaosWorld;
import com.yongaishide.chaosworld.mekanism.inventory.container.MechanicalAssemblerContainer;
import com.yongaishide.chaosworld.mekanism.recipe.AssemblingRecipe;
import com.yongaishide.chaosworld.mekanism.recipe.AssemblingRecipeSerializer;
import com.yongaishide.chaosworld.mekanism.recipe.ChaosRecipeType;
import com.yongaishide.chaosworld.mekanism.recipe.ChemicalInputCache;
import com.yongaishide.chaosworld.mekanism.tile.TileEntityMechanicalAssembler;
import mekanism.common.content.blocktype.Machine.MachineBuilder;
import mekanism.common.block.prefab.BlockTile.BlockTileModel;
import mekanism.common.content.blocktype.BlockShapes;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.registries.MekanismSounds;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 机械组装机(Mechanical Assembler)注册中心。
 * <p>
 * 完全仿照 MEK 结晶器的注册链路:
 * {@code Machine(BlockType) -> BlockTileModel + ItemBlockTooltip -> TileEntityType -> ContainerType -> RecipeType/Serializer}
 */
public final class MekanismMachines {

    private MekanismMachines() {
    }

    //===== MEK 风格的注册器 =====
    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(ChaosWorld.MODID);
    public static final TileEntityTypeDeferredRegister TILE_ENTITY_TYPES = new TileEntityTypeDeferredRegister(ChaosWorld.MODID);
    public static final ContainerTypeDeferredRegister CONTAINER_TYPES = new ContainerTypeDeferredRegister(ChaosWorld.MODID);

    //===== 原版注册器(配方) =====
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, ChaosWorld.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, ChaosWorld.MODID);

    //===== 配方类型:chaosworld_core:assembling =====
    public static final String ASSEMBLING_ID = "assembling";
    public static final ChaosRecipeType<RecipeInput, AssemblingRecipe, ChemicalInputCache<AssemblingRecipe>> ASSEMBLING =
          new ChaosRecipeType<>(ChaosWorld.id(ASSEMBLING_ID), ChemicalInputCache::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<?>> ASSEMBLING_TYPE =
          RECIPE_TYPES.register(ASSEMBLING_ID, () -> ASSEMBLING);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> ASSEMBLING_SERIALIZER =
          RECIPE_SERIALIZERS.register(ASSEMBLING_ID, () -> AssemblingRecipeSerializer.INSTANCE);

    //===== 方块类型(仿照 MekanismBlockTypes.CHEMICAL_CRYSTALLIZER) =====
    public static final Machine<TileEntityMechanicalAssembler> MECHANICAL_ASSEMBLER_TYPE = MachineBuilder
          .createMachine(MekanismMachines::tileType, new MechanicalAssemblerLang("mechanical_assembler"))
          .withGui(MekanismMachines::containerType)
          .withSound(MekanismSounds.CHEMICAL_CRYSTALLIZER)
          .withEnergyConfig(() -> 50L, () -> 20_000L)
          .withSideConfig(TransmissionType.ITEM, TransmissionType.CHEMICAL, TransmissionType.FLUID, TransmissionType.ENERGY)
          .withCustomShape(BlockShapes.PRESSURIZED_REACTION_CHAMBER)
          .build();

    //===== 方块注册 =====
    public static final BlockRegistryObject<BlockTileModel<TileEntityMechanicalAssembler, Machine<TileEntityMechanicalAssembler>>,
          ItemBlockTooltip<BlockTileModel<TileEntityMechanicalAssembler, Machine<TileEntityMechanicalAssembler>>>> MECHANICAL_ASSEMBLER =
          BLOCKS.register("mechanical_assembler",
                () -> new BlockTileModel<>(MECHANICAL_ASSEMBLER_TYPE, properties -> properties.mapColor(MapColor.METAL)),
                (block, properties) -> new ItemBlockTooltip<>(block, true, properties));

    //===== 方块实体注册 =====
    public static final TileEntityTypeRegistryObject<TileEntityMechanicalAssembler> MECHANICAL_ASSEMBLER_TILE = TILE_ENTITY_TYPES
          .mekBuilder(MECHANICAL_ASSEMBLER, TileEntityMechanicalAssembler::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .build();

    //===== 容器注册 =====
    public static final ContainerTypeRegistryObject<MechanicalAssemblerContainer> MECHANICAL_ASSEMBLER_CONTAINER =
          CONTAINER_TYPES.register("mechanical_assembler", TileEntityMechanicalAssembler.class,
                MekanismMachines::createMechanicalAssemblerContainer);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        TILE_ENTITY_TYPES.register(eventBus);
        CONTAINER_TYPES.register(eventBus);
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }

    //静态字段存在循环引用(TYPE -> TILE -> BLOCK -> TYPE),用访问器方法绕过 javac 的前向引用检查
    private static TileEntityTypeRegistryObject<TileEntityMechanicalAssembler> tileType() {
        return MECHANICAL_ASSEMBLER_TILE;
    }

    public static ContainerTypeRegistryObject<MechanicalAssemblerContainer> containerType() {
        return MECHANICAL_ASSEMBLER_CONTAINER;
    }

    private static MechanicalAssemblerContainer createMechanicalAssemblerContainer(int windowId,
          net.minecraft.world.entity.player.Inventory inv, TileEntityMechanicalAssembler tile) {
        return new MechanicalAssemblerContainer(containerType(), windowId, inv, tile);
    }
}
