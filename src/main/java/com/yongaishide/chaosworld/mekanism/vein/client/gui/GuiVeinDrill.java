package com.yongaishide.chaosworld.mekanism.vein.client.gui;

import java.util.List;
import com.yongaishide.chaosworld.mekanism.vein.VeinData;
import com.yongaishide.chaosworld.mekanism.vein.VeinDrillContainer;
import com.yongaishide.chaosworld.mekanism.vein.VeinSize;
import com.yongaishide.chaosworld.mekanism.vein.tile.TileEntityVeinDrill;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiDigitalSwitch;
import mekanism.client.gui.element.GuiDigitalSwitch.SwitchType;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.button.TranslationButton;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * 虚脉钻探机 GUI(数字采矿机布局):信息屏 + 4 开关 + 扫描/修复按钮 + 12x5 输出网格 + 能量条。
 * 窗口 250x298;玩家物品栏在 y=206(见 VeinDrillContainer.INVENTORY_Y_OFFSET)。
 * 注意:开关图标真实路径为 mekanism:gui/switch/*.png。数字采矿机的扫描是文字按钮,这里用翻译按钮。
 */
public class GuiVeinDrill extends GuiMekanismTile<TileEntityVeinDrill, VeinDrillContainer> {

    public GuiVeinDrill(VeinDrillContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
        //背景与内容齐平(9 列网格 170 + 边距),消除右侧空白
        imageWidth = 176;
        imageHeight = 298;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();

        //信息屏(7,19,128,72):状态行(矿脉等级/质量剩余/耐久/缺失提示)
        addRenderableWidget(new GuiInnerScreen(this, 7, 19, 128, 72, this::screenLines)
              .clearSpacing()
              .clearFormat());

        //2 个开关(自动弹出/自动拉取;自动扫描与自动维护已常开)
        //与数字采矿机一致:开关点击发送 PacketEjectConfiguration 到服务端,由侧配置 ejecting 标志驱动 ejector
        addSwitch(11, "eject.png", () -> tile.isAutoEject(),
              () -> mekanism.common.network.PacketUtils.sendToServer(
                    new mekanism.common.network.to_server.configuration_update.PacketEjectConfiguration(tile.getBlockPos(),
                          mekanism.common.lib.transmitter.TransmissionType.ITEM)), "auto_eject");
        addSwitch(27, "input.png", () -> tile.isAutoPull(), tile::toggleAutoPull, "auto_pull");
        addSwitch(43, "silk.png", () -> tile.isAutoRepair(), tile::toggleAutoRepair, "auto_repair");

        //手动修复按钮(修复槽旁,点击消耗 1 件修复品)
        addRenderableWidget(new TranslationButton(this, 75, 92, 44, 18, entry("info.chaosworld_core.vein.repair"),
              (el, mx, my) -> {
                  tile.repair();
                  return true;
              }));

        //能量条(与数字采矿机一致:能量槽下方,157,39 高 47);能量不足 单次挖掘开销 时挂警告
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 157, 39, 47))
              .warning(mekanism.common.inventory.warning.WarningTracker.WarningType.NOT_ENOUGH_ENERGY, () -> {
                  var energyContainer = tile.getEnergyContainer();
                  return energyContainer.getEnergy() < tile.getEnergyPerMineForWarning();
              });

        //能量信息书签 tab(左下,显示每 tick 平均能量消耗)
        addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), tile::getEnergyPerTickMilli));

        //三个输入槽同排对齐(核心/科技锭/修复):7,92 / 27,92 / 47,92
        addRenderableWidget(new GuiSlot(SlotType.INNER_HOLDER_SLOT, this, 7, 92));
        addRenderableWidget(new GuiSlot(SlotType.INPUT_2, this, 27, 92));
        addRenderableWidget(new GuiSlot(SlotType.INPUT_2, this, 47, 92));

        //独立升级页:新增"钻头/矿脉"升级书签 tab(标准升级 tab 由 Mekanism 通过 AttributeUpgradeSupport 自动添加)
        addRenderableWidget(new VeinUpgradeTab(this, tile));
    }

    private long displayedDurability() {
        //tracker 已同步(>=0)用真值;未同步回退 tile.getDurability()(该值由 durability tracker 保证同步)
        return tile.viewDurability >= 0 ? tile.viewDurability : tile.getDurability();
    }

    private List<Component> screenLines() {
        if (tile.viewVeinSizeId < 0) {
            return List.of(
                  Component.translatable("info.chaosworld_core.vein.missing"),
                  Component.translatable("info.chaosworld_core.vein.durability", displayedDurability(),
                        tile.isInfiniteDrill() ? Component.translatable("info.chaosworld_core.vein.upgrade.infinite") : tile.getMaxDurability()));
        }
        return List.of(
              Component.translatable("info.chaosworld_core.vein.size",
                    Component.translatable("vein.size." + VeinSize.byId(tile.viewVeinSizeId).getName())),
              Component.translatable("info.chaosworld_core.vein.mass",
                    formatMass(tile.viewMassRemaining), formatMass(tile.viewMassCapacity)),
              Component.translatable("info.chaosworld_core.vein.durability", displayedDurability(),
                    tile.isInfiniteDrill() ? Component.translatable("info.chaosworld_core.vein.upgrade.infinite") : tile.getMaxDurability()));
    }

    private static String formatMass(long mass) {
        if (mass >= 1_000_000) {
            return String.format("%.1fM", mass / 1_000_000.0);
        } else if (mass >= 1_000) {
            return String.format("%.1fK", mass / 1_000.0);
        }
        return String.valueOf(mass);
    }

    private void addSwitch(int x, String texture, java.util.function.BooleanSupplier supplier,
          Runnable toggler, String langKey) {
        addRenderableWidget(new GuiDigitalSwitch(this, x, 56,
              ResourceLocation.fromNamespaceAndPath("mekanism", "gui/switch/" + texture),
              supplier, (el, mx, my) -> {
                  toggler.run();
                  return true;
              }, SwitchType.LOWER_ICON))
              .setTooltip(entry("info.chaosworld_core.vein." + langKey));
    }

    private static mekanism.api.text.ILangEntry entry(String key) {
        return new mekanism.api.text.ILangEntry() {
            @Override
            public String getTranslationKey() {
                return key;
            }
        };
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
