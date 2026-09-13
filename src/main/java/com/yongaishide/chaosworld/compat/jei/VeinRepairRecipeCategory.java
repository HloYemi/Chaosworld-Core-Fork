package com.yongaishide.chaosworld.compat.jei;

import java.util.ArrayList;
import java.util.List;
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
 * 修复品 JEI 页:左=修复品,中=锤子(代表钻头),右=修复后的耐久;下方"修复值: N"。
 */
public class VeinRepairRecipeCategory implements IRecipeCategory<VeinRepairRecipeInfo> {

    public static final RecipeType<VeinRepairRecipeInfo> RECIPE_TYPE =
          RecipeType.create("chaosworld_core", "vein_repair", VeinRepairRecipeInfo.class);

    private static final int WIDTH = 172;
    private static final int HEIGHT = 92;

    private final IDrawable icon;
    private final IDrawable background;

    public VeinRepairRecipeCategory(IJeiHelpers helpers) {
        var guiHelper = helpers.getGuiHelper();
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.IRON_INGOT));
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<VeinRepairRecipeInfo> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.chaosworld_core.vein_repair.title");
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
    public void setRecipe(IRecipeLayoutBuilder builder, VeinRepairRecipeInfo recipe, IFocusGroup focuses) {
        //中间:修复品图标
        builder.addOutputSlot(WIDTH / 2 - 10, 30).addItemStack(recipe.item())
              .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.chaosworld_core.vein_repair.repair_item")));
    }

    @Override
    public void draw(VeinRepairRecipeInfo recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        gfx.drawString(font, Component.translatable("jei.chaosworld_core.vein_repair.amount", recipe.repairAmount()).getString(),
              WIDTH / 4 - 20, HEIGHT - 18, 0x333333);
    }

    /** 从数据包 repair_items 生成当前锤子对应的修复值条目;读不到时兜底铁锭 25 */
    public static List<VeinRepairRecipeInfo> all() {
        List<VeinRepairRecipeInfo> list = new ArrayList<>();
        try {
            //直接读客户端资源(不依赖服务端运行时 loader),与矿石池 JEI 同法
            var resourceManager = Minecraft.getInstance().getResourceManager();
            var resources = resourceManager.listResources("vein_drill/repair_items.json", p -> p.getPath().endsWith(".json"));
            for (var entry : resources.entrySet()) {
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(entry.getValue().open()))) {
                    var obj = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                    for (com.google.gson.JsonElement e : obj.getAsJsonArray("items")) {
                        var itemObj = e.getAsJsonObject();
                        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemObj.get("id").getAsString()));
                        if (item != Items.AIR) {
                            int amount = itemObj.has("repair") ? itemObj.get("repair").getAsInt() : 25;
                            list.add(new VeinRepairRecipeInfo(new ItemStack(item), amount));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            mekanism.common.Mekanism.logger.error("Failed to build vein repair JEI pages", ex);
        }
        if (list.isEmpty()) {
            list.add(new VeinRepairRecipeInfo(new ItemStack(Items.IRON_INGOT), 25));
        }
        return list;
    }
}
