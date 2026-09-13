package com.yongaishide.chaosworld.compat.jei;

import java.util.List;
import com.yongaishide.chaosworld.mekanism.vein.OreEntry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 虚脉钻探机 JEI 页:按维度展示矿石池的默认产物(物品/流体/气体)+ 权重概率。
 */
public class VeinDrillRecipeCategory implements IRecipeCategory<VeinDrillPoolInfo> {

    public static final RecipeType<VeinDrillPoolInfo> RECIPE_TYPE =
            RecipeType.create("chaosworld_core", "vein_drill", VeinDrillPoolInfo.class);

    private static final int WIDTH = 172;
    private static final int HEIGHT = 128;

    private final IDrawable icon;
    private final IDrawable background;

    public VeinDrillRecipeCategory(IJeiHelpers helpers) {
        var guiHelper = helpers.getGuiHelper();
        this.icon = guiHelper.createDrawableItemStack(
              com.yongaishide.chaosworld.mekanism.vein.VeinDrillMachines.VEIN_DRILL.asItem().getDefaultInstance());
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<VeinDrillPoolInfo> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.chaosworld_core.vein_drill.title");
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, VeinDrillPoolInfo recipe, IFocusGroup focuses) {
        //单页显示全部条目(池 19 条以内,8x3 网格)
        int poolSize = (int) java.util.stream.IntStream.range(0, recipe.entries().size()).mapToLong(i -> recipe.entries().get(i).weight()).sum();
        for (int i = 0; i < recipe.entries().size(); i++) {
            OreEntry entry = recipe.entries().get(i);
            int col = i % 8;
            int row = i / 8;
            int x = 10 + col * 19;
            int y = 27 + row * 19;
            switch (entry.type()) {
                case ITEM -> {
                    ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.id())));
                    if (stack.isEmpty()) {
                        stack = new ItemStack(Items.COAL);
                    }
                    builder.addOutputSlot(x, y).addItemStack(stack)
                          .addRichTooltipCallback((view, tooltip) -> poolTooltip(recipe, entry, poolSize, view, tooltip));
                }
                case FLUID -> {
                    var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(entry.id()));
                    builder.addOutputSlot(x, y).addFluidStack(fluid, 1000)
                          .addRichTooltipCallback((view, tooltip) -> poolTooltip(recipe, entry, poolSize, view, tooltip));
                }
                case GAS -> {
                    //气体无原生 JEI 类型,用气体罐占位并附全量信息
                    builder.addOutputSlot(x, y).addItemStack(gasPlaceholder())
                          .addRichTooltipCallback((view, tooltip) -> poolTooltip(recipe, entry, poolSize, view, tooltip));
                }
            }
        }
    }

    public static ItemStack gasPlaceholder() {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:gas_canister"));
        return item != Items.AIR ? new ItemStack(item) : new ItemStack(Items.FLINT_AND_STEEL);
    }

    /**
     * 直接从客户端资源管理器读取 data/chaosworld_core/vein_drill/ore_pools/*.json 合并为统一池;
     * 读不到(时序/打包问题)时回退到内置默认池,保证 JEI 一定有产出预览。
     */
    public static List<VeinDrillPoolInfo> allDimensions() {
        List<VeinDrillPoolInfo> pools = new java.util.ArrayList<>();
        java.util.Map<OreEntry, Integer> merged = new java.util.LinkedHashMap<>();
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            var resources = resourceManager.listResources("vein_drill/ore_pools", p -> p.getPath().endsWith(".json"));
            for (var entry : resources.entrySet()) {
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(entry.getValue().open()))) {
                    var obj = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                    for (com.google.gson.JsonElement e : obj.getAsJsonArray("entries")) {
                        OreEntry ore = OreEntry.parse(e.getAsJsonObject());
                        merged.merge(ore, ore.weight(), Integer::sum);
                    }
                }
            }
        } catch (Exception ex) {
            mekanism.common.Mekanism.logger.error("Failed to load vein drill pools for JEI", ex);
        }
        if (merged.isEmpty()) {
            mekanism.common.Mekanism.logger.warn("Vein drill pools empty; falling back to built-in defaults");
            for (VeinDrillPoolInfo pool : fallbackPools()) {
                for (OreEntry ore : pool.entries()) {
                    merged.merge(ore, ore.weight(), Integer::sum);
                }
            }
        }
        if (!merged.isEmpty()) {
            //统一池:单页显示全部(池 19 条以内可直接展示)
            pools.add(new VeinDrillPoolInfo(ResourceLocation.parse("chaosworld_core:unified"),
                  new java.util.ArrayList<>(merged.keySet()), 0, 1));
        }
        return pools;
    }

    /** 内置兜底池(与默认数据包一致,维度已合并) */
    static List<VeinDrillPoolInfo> fallbackPools() {
        return List.of(new VeinDrillPoolInfo(ResourceLocation.parse("chaosworld_core:unified"), List.of(
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:coal", 30, 1, 1),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:raw_iron", 25, 2, 1),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:raw_copper", 20, 2, 1),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:redstone", 15, 1, 1),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:lapis_lazuli", 10, 3, 1),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:raw_gold", 10, 4, 2),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:emerald", 3, 10, 3),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:diamond", 2, 20, 4),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:nether_quartz", 40, 1, 3),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:gold_nugget", 20, 4, 4),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:glowstone_dust", 12, 2, 5),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:blaze_powder", 10, 3, 6),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:netherite_scrap", 4, 40, 7),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:ancient_debris", 1, 80, 8),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:netherite_ingot", 1, 100, 9),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:obsidian", 20, 3, 6),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:ender_pearl", 25, 8, 7),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:shulker_shell", 4, 30, 8),
              new OreEntry(OreEntry.PoolType.ITEM, "minecraft:dragon_breath", 2, 50, 9))));
    }

    private void poolTooltip(VeinDrillPoolInfo recipe, OreEntry entry, int total, IRecipeSlotView view,
          ITooltipBuilder tooltip) {
        float pct = total <= 0 ? 0F : entry.weight() * 100F / total;
        tooltip.add(Component.translatable("jei.chaosworld_core.vein_drill.weight",
              String.format("%.1f%%", pct)));
        tooltip.add(Component.translatable("jei.chaosworld_core.vein_drill.quality_cost", entry.qualityCost()));
        tooltip.add(Component.translatable("jei.chaosworld_core.vein_drill.min_level", entry.minDrillLevel()));
        if (entry.type() == OreEntry.PoolType.GAS) {
            tooltip.add(Component.translatable("jei.chaosworld_core.vein_drill.gas", entry.id()));
        }
    }

    @Override
    public void draw(VeinDrillPoolInfo recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        gfx.drawCenteredString(font, Component.translatable("jei.chaosworld_core.vein_drill.unified").getString(),
              WIDTH / 2, 6, 0x333333);
    }
}
