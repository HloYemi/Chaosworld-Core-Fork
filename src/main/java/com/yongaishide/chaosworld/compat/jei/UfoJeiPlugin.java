package com.yongaishide.chaosworld.compat.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.moakiee.ae2lt.integration.jei.category.MultiblockStructureCategory;
import com.moakiee.ae2lt.registry.ModRecipeTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import com.yongaishide.chaosworld.block.MultiblockBlocks;
import com.yongaishide.chaosworld.block.ModBlocks;
import com.yongaishide.chaosworld.recipe.DimensionalMatterAssemblerRecipe;
import com.yongaishide.chaosworld.recipe.QMFRecipe;
import com.yongaishide.chaosworld.recipe.UniversalMultiblockMachineKind;
import com.yongaishide.chaosworld.recipe.UniversalMultiblockRecipe;

import net.pedroksl.ae2addonlib.recipes.IngredientStack;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

@JeiPlugin
public class UfoJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("chaosworld_core", "jei_plugin");
    private static IJeiRuntime runtime;

    public UfoJeiPlugin() {}

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        var jeiHelpers = registry.getJeiHelpers();
        registry.addRecipeCategories(new DimensionalMatterAssemblerRecipeCategory(jeiHelpers));
        registry.addRecipeCategories(new UniversalMultiblockRecipeCategory(
                jeiHelpers,
                UniversalMultiblockMachineKind.QMF,
                MultiblockBlocks.QUANTUM_MATTER_FABRICATOR_CONTROLLER.get().asItem().getDefaultInstance(),
                MultiblockBlocks.QUANTUM_MATTER_FABRICATOR_CONTROLLER.get().getName()));
        registry.addRecipeCategories(new UniversalMultiblockRecipeCategory(
                jeiHelpers,
                UniversalMultiblockMachineKind.QUANTUM_SLICER,
                MultiblockBlocks.QUANTUM_SLICER_CONTROLLER.get().asItem().getDefaultInstance(),
                MultiblockBlocks.QUANTUM_SLICER_CONTROLLER.get().getName()));
        registry.addRecipeCategories(new UniversalMultiblockRecipeCategory(
                jeiHelpers,
                UniversalMultiblockMachineKind.QUANTUM_PROCESSOR_ASSEMBLER,
                MultiblockBlocks.QUANTUM_PROCESSOR_ASSEMBLER_CONTROLLER.get().asItem().getDefaultInstance(),
                MultiblockBlocks.QUANTUM_PROCESSOR_ASSEMBLER_CONTROLLER.get().getName()));
        registry.addRecipeCategories(new UniversalMultiblockRecipeCategory(
                jeiHelpers,
                UniversalMultiblockMachineKind.QUANTUM_CRYOFORGE,
                MultiblockBlocks.QUANTUM_CRYOFORGE_CONTROLLER.get().asItem().getDefaultInstance(),
                MultiblockBlocks.QUANTUM_CRYOFORGE_CONTROLLER.get().getName()));
        registry.addRecipeCategories(new StellarSimulationRecipeCategory(jeiHelpers));
        registry.addRecipeCategories(new WitherSummonRecipeCategory(jeiHelpers));
        registry.addRecipeCategories(new AssemblingRecipeCategory(jeiHelpers));
        registry.addRecipeCategories(new DragonSoulForgingRecipeCategory(jeiHelpers));
        registry.addRecipeCategories(new VeinDrillRecipeCategory(jeiHelpers));
        registry.addRecipeCategories(new VeinRepairRecipeCategory(jeiHelpers));
        registry.addRecipeCategories(new FusionConversionRecipeCategory(jeiHelpers));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        registration.addRecipes(
                DimensionalMatterAssemblerRecipeCategory.RECIPE_TYPE,
                List.copyOf(recipeManager.getAllRecipesFor(com.yongaishide.chaosworld.init.ModRecipes.DMA_RECIPE_TYPE.get()).stream()
                        .map(RecipeHolder::value)
                        .toList()));
        registration.addRecipes(MultiblockStructureCategory.TYPE, ChaosMultiblockStructures.all());
        var universalRecipes = List.copyOf(recipeManager.getAllRecipesFor(com.yongaishide.chaosworld.init.ModRecipes.UNIVERSAL_MULTIBLOCK_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .toList());
        List<UniversalMultiblockRecipe> qmfRecipes = new ArrayList<>(
                universalRecipes.stream().filter(recipe -> recipe.getMachine() == UniversalMultiblockMachineKind.QMF).toList());
        for (var holder : recipeManager.getAllRecipesFor(com.yongaishide.chaosworld.init.ModRecipes.DMA_RECIPE_TYPE.get())) {
            qmfRecipes.add(UniversalMultiblockRecipe.fromDma(holder.id(), holder.value()));
        }
        registration.addRecipes(UniversalMultiblockRecipeCategory.QMF_RECIPE_TYPE, qmfRecipes);
        List<UniversalMultiblockRecipe> slicerRecipes = new ArrayList<>(
                universalRecipes.stream().filter(recipe -> recipe.getMachine() == UniversalMultiblockMachineKind.QUANTUM_SLICER).toList());
        if (net.neoforged.fml.ModList.get().isLoaded("extendedae")) {
            try {
                for (var holder : recipeManager.getAllRecipesFor(com.glodblock.github.extendedae.recipe.CircuitCutterRecipe.TYPE)) {
                    slicerRecipes.add(UniversalMultiblockRecipe.fromCircuitCutter(holder.id(), holder.value()));
                }
            } catch (Throwable ignored) {
                // JEI display only; never break recipe registration on compat issues
            }
        }
        registration.addRecipes(
                UniversalMultiblockRecipeCategory.QUANTUM_SLICER_RECIPE_TYPE,
                slicerRecipes);
        List<UniversalMultiblockRecipe> quantumProcessingFactoryRecipes = new ArrayList<>(
                universalRecipes.stream().filter(recipe -> recipe.getMachine() == UniversalMultiblockMachineKind.QUANTUM_PROCESSOR_ASSEMBLER).toList());
        for (var holder : recipeManager.getAllRecipesFor(ModRecipeTypes.OVERLOAD_PROCESSING_TYPE.get())) {
            var recipe = holder.value();
            if (!recipe.isIncomplete()) {
                quantumProcessingFactoryRecipes.add(UniversalMultiblockRecipe.fromOverload(holder.id(), recipe));
            }
        }
        registration.addRecipes(
                UniversalMultiblockRecipeCategory.QUANTUM_PROCESSOR_ASSEMBLER_RECIPE_TYPE,
                quantumProcessingFactoryRecipes);
        registration.addRecipes(
                UniversalMultiblockRecipeCategory.QUANTUM_CRYOFORGE_RECIPE_TYPE,
                universalRecipes.stream().filter(recipe -> recipe.getMachine() == UniversalMultiblockMachineKind.QUANTUM_CRYOFORGE).toList());
        registration.addRecipes(
                StellarSimulationRecipeCategory.RECIPE_TYPE,
                List.copyOf(recipeManager.getAllRecipesFor(com.yongaishide.chaosworld.init.ModRecipes.STELLAR_SIMULATION_TYPE.get()).stream()
                        .map(RecipeHolder::value)
                        .toList()));
        registration.addRecipes(
                WitherSummonRecipeCategory.RECIPE_TYPE,
                List.of(new WitherSummonInfo()));
        //机械组装机(assembling)配方
        registration.addRecipes(
                AssemblingRecipeCategory.RECIPE_TYPE,
                List.copyOf(recipeManager.getAllRecipesFor(com.yongaishide.chaosworld.mekanism.MekanismMachines.ASSEMBLING).stream()
                        .map(RecipeHolder::value)
                        .filter(com.yongaishide.chaosworld.mekanism.recipe.BasicAssemblingRecipe.class::isInstance)
                        .map(com.yongaishide.chaosworld.mekanism.recipe.BasicAssemblingRecipe.class::cast)
                        .toList()));
        //龙魂锻炉(dragon_soul_forging)配方
        registration.addRecipes(
                DragonSoulForgingRecipeCategory.RECIPE_TYPE,
                List.copyOf(recipeManager.getAllRecipesFor(
                        com.yongaishide.chaosworld.mekanism.DragonSoulForgeMachines.DRAGON_SOUL_FORGING).stream()
                        .map(RecipeHolder::value)
                        .filter(com.yongaishide.chaosworld.mekanism.recipe.BasicDragonSoulForgingRecipe.class::isInstance)
                        .map(com.yongaishide.chaosworld.mekanism.recipe.BasicDragonSoulForgingRecipe.class::cast)
                        .toList()));
        //虚脉钻探机:各维度矿石池(直接读客户端资源,不依赖服务端运行时 loader)
        List<VeinDrillPoolInfo> veinPools = com.yongaishide.chaosworld.compat.jei.VeinDrillRecipeCategory.allDimensions();
        registration.addRecipes(VeinDrillRecipeCategory.RECIPE_TYPE, veinPools);
        //修复品:数据包 repair_items
        registration.addRecipes(VeinRepairRecipeCategory.RECIPE_TYPE, VeinRepairRecipeCategory.all());
        //聚变转化:数据包 fusion_conversion
        registration.addRecipes(
                FusionConversionRecipeCategory.RECIPE_TYPE,
                List.copyOf(recipeManager.getAllRecipesFor(com.yongaishide.chaosworld.init.ModRecipes.FUSION_CONVERSION_TYPE.get()).stream()
                        .map(RecipeHolder::value)
                        .toList()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        var dmaBlock = ModBlocks.DIMENSIONAL_MATTER_ASSEMBLER_BLOCK.get().asItem().getDefaultInstance();
        registration.addRecipeCatalyst(dmaBlock, DimensionalMatterAssemblerRecipeCategory.RECIPE_TYPE);

        var nexusController = MultiblockBlocks.STELLAR_NEXUS_CONTROLLER.get().asItem().getDefaultInstance();
        registration.addRecipeCatalyst(nexusController, StellarSimulationRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(nexusController, MultiblockStructureCategory.TYPE);

        var qmfController = MultiblockBlocks.QUANTUM_MATTER_FABRICATOR_CONTROLLER.get().asItem().getDefaultInstance();
        registration.addRecipeCatalyst(qmfController, QmfRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(qmfController, UniversalMultiblockRecipeCategory.QMF_RECIPE_TYPE);
        registration.addRecipeCatalyst(qmfController, MultiblockStructureCategory.TYPE);

        var quantumSlicerController = MultiblockBlocks.QUANTUM_SLICER_CONTROLLER.get().asItem().getDefaultInstance();
        registration.addRecipeCatalyst(quantumSlicerController, UniversalMultiblockRecipeCategory.QUANTUM_SLICER_RECIPE_TYPE);
        registration.addRecipeCatalyst(quantumSlicerController, MultiblockStructureCategory.TYPE);

        var quantumProcessorAssemblerController = MultiblockBlocks.QUANTUM_PROCESSOR_ASSEMBLER_CONTROLLER.get().asItem().getDefaultInstance();
        registration.addRecipeCatalyst(quantumProcessorAssemblerController, UniversalMultiblockRecipeCategory.QUANTUM_PROCESSOR_ASSEMBLER_RECIPE_TYPE);
        registration.addRecipeCatalyst(quantumProcessorAssemblerController, MultiblockStructureCategory.TYPE);

        var quantumCryoforgeController = MultiblockBlocks.QUANTUM_CRYOFORGE_CONTROLLER.get().asItem().getDefaultInstance();
        registration.addRecipeCatalyst(quantumCryoforgeController, UniversalMultiblockRecipeCategory.QUANTUM_CRYOFORGE_RECIPE_TYPE);
        registration.addRecipeCatalyst(quantumCryoforgeController, MultiblockStructureCategory.TYPE);

        registration.addRecipeCatalyst(
                com.yongaishide.chaosworld.mekanism.MekanismMachines.MECHANICAL_ASSEMBLER.asItem().getDefaultInstance(),
                AssemblingRecipeCategory.RECIPE_TYPE);
        //8 个工厂等级按顺序加入配方预览催化剂
        var factoryBlocks = com.yongaishide.chaosworld.mekanism.AssemblingFactoryMachines.FACTORY_BLOCKS;
        for (int i = 0; i < com.yongaishide.chaosworld.mekanism.AssemblingFactoryMachines.getCount(); i++) {
            registration.addRecipeCatalyst(factoryBlocks[i].asItem().getDefaultInstance(), AssemblingRecipeCategory.RECIPE_TYPE);
        }
        //龙魂锻炉:单机 + 8 个工厂等级作为催化剂
        var forgeBlocks = com.yongaishide.chaosworld.mekanism.DragonSoulForgeMachines.FACTORY_BLOCKS;
        registration.addRecipeCatalyst(
                com.yongaishide.chaosworld.mekanism.DragonSoulForgeMachines.DRAGON_SOUL_FORGE.asItem().getDefaultInstance(),
                DragonSoulForgingRecipeCategory.RECIPE_TYPE);
        for (int i = 0; i < com.yongaishide.chaosworld.mekanism.DragonSoulForgeMachines.getCount(); i++) {
            registration.addRecipeCatalyst(forgeBlocks[i].asItem().getDefaultInstance(), DragonSoulForgingRecipeCategory.RECIPE_TYPE);
        }
        registration.addRecipeCatalyst(
                com.yongaishide.chaosworld.mekanism.vein.VeinDrillMachines.VEIN_DRILL.asItem().getDefaultInstance(),
                VeinDrillRecipeCategory.RECIPE_TYPE);
        //修复值页仅从虚脉钻探机进入(JEI 搜铁锭等不会出现该类别)
        registration.addRecipeCatalyst(
                com.yongaishide.chaosworld.mekanism.vein.VeinDrillMachines.VEIN_DRILL.asItem().getDefaultInstance(),
                VeinRepairRecipeCategory.RECIPE_TYPE);
        //聚变转化:Mekanism 聚变反应堆控制器(仅在该模组存在时注册)
        var fusionController = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("mekanismgenerators", "fusion_reactor_controller"));
        if (fusionController != net.minecraft.world.item.Items.AIR) {
            registration.addRecipeCatalyst(new ItemStack(fusionController), FusionConversionRecipeCategory.RECIPE_TYPE);
        }
    }

    public static ItemStack getHoveredItemStack() {
        if (runtime == null) {
            return ItemStack.EMPTY;
        }
        return runtime.getRecipesGui().getIngredientUnderMouse(VanillaTypes.ITEM_STACK)
                .or(() -> runtime.getIngredientListOverlay().getIngredientUnderMouse().flatMap(ingredient -> ingredient.getItemStack()))
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
    }

    public static Ingredient stackOf(IngredientStack.Item stack) {
        if (!stack.isEmpty()) {
            return Ingredient.of(Arrays.stream(stack.getIngredient().getItems())
                    .map(oldStack -> oldStack.copyWithCount(stack.getAmount())));
        }
        return Ingredient.of(ItemStack.EMPTY);
    }

    public static List<FluidStack> stackOf(IngredientStack.Fluid stack) {
        FluidIngredient ingredient = stack.getIngredient();
        return Arrays.stream(ingredient.getStacks())
                .map(oldStack -> oldStack.copyWithAmount(stack.getAmount()))
                .toList();
    }

    public static Ingredient stackOfQmf(QMFRecipe.QMFRecipeIngredient stack) {
        if (stack != null && !stack.ingredient().isEmpty() && stack.amount() > 0) {
            return Ingredient.of(Arrays.stream(stack.ingredient().getItems())
                    .map(oldStack -> oldStack.copyWithCount((int) Math.min(Integer.MAX_VALUE, stack.amount()))));
        }
        return Ingredient.of(ItemStack.EMPTY);
    }

    public static Ingredient stackOfUniversal(UniversalMultiblockRecipe.ItemRequirement stack) {
        if (stack != null && !stack.ingredient().isEmpty() && stack.amount() > 0) {
            return Ingredient.of(Arrays.stream(stack.ingredient().getItems())
                    .map(oldStack -> oldStack.copyWithCount(1)));
        }
        return Ingredient.of(ItemStack.EMPTY);
    }
}
