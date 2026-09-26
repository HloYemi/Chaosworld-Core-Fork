package com.yongaishide.chaosworld.mixin.ufo.client;

import appeng.api.config.YesNo;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import com.extendedae_plus.api.config.EAPSettings;
import com.extendedae_plus.client.gui.widgets.EAPServerSettingToggleButton;
import com.extendedae_plus.util.GuiUtil;
import com.raishxn.ufo.screen.QuantumPatternHatchMenu;
import com.yongaishide.chaosworld.patch.contents.PatchPatternHatchMenu;
import com.raishxn.ufo.screen.QuantumPatternHatchScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Patch layer: adds the ExtendedAE+ advanced blocking / smart doubling toggles
 * and the per-provider scaling limit input to the Quantum Pattern Hatch screen.
 */
@Mixin(value = QuantumPatternHatchScreen.class, remap = false)
public abstract class QuantumPatternHatchScreenMixin extends AEBaseScreen<QuantumPatternHatchMenu> {

    @Unique
    private EAPServerSettingToggleButton<YesNo> chaosworld$advancedToggle;

    @Unique
    private EAPServerSettingToggleButton<YesNo> chaosworld$smartToggle;

    @Unique
    private AETextField chaosworld$limitInput;

    @Unique
    private int chaosworld$limit;

    public QuantumPatternHatchScreenMixin(QuantumPatternHatchMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void chaosworld$addWidgets(QuantumPatternHatchMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style, CallbackInfo ci) {
        this.chaosworld$advancedToggle = new EAPServerSettingToggleButton<>(EAPSettings.ADVANCED_BLOCKING, YesNo.YES);
        this.chaosworld$smartToggle = new EAPServerSettingToggleButton<>(EAPSettings.SMART_DOUBLING, YesNo.YES);
        this.addToLeftToolbar(this.chaosworld$advancedToggle);
        this.addToLeftToolbar(this.chaosworld$smartToggle);

        this.chaosworld$limit = ((PatchPatternHatchMenu) this.getMenu()).chaosworld$getPerProviderScalingLimit();
        this.chaosworld$limitInput = GuiUtil.createPerProviderLimitInput(this.style, this.font, this.chaosworld$limit,
                value -> {
                    this.chaosworld$limit = value;
                    ((PatchPatternHatchMenu) this.getMenu()).chaosworld$sendScalingLimitFromClient(value);
                });
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"), remap = false)
    private void chaosworld$updateWidgets(CallbackInfo ci) {
        QuantumPatternHatchMenu menu = this.getMenu();
        if (this.chaosworld$advancedToggle != null) {
            this.chaosworld$advancedToggle.set(((PatchPatternHatchMenu) menu).chaosworld$getAdvancedBlocking());
        }
        if (this.chaosworld$smartToggle != null) {
            this.chaosworld$smartToggle.set(((PatchPatternHatchMenu) menu).chaosworld$getSmartDoubling());
        }
        if (this.chaosworld$limitInput == null) {
            return;
        }

        if (((PatchPatternHatchMenu) menu).chaosworld$getSmartDoubling() == YesNo.YES) {
            if (!this.renderables.contains(this.chaosworld$limitInput)) {
                this.addRenderableWidget(this.chaosworld$limitInput);
            }
            int limit = ((PatchPatternHatchMenu) menu).chaosworld$getPerProviderScalingLimit();
            if (!this.chaosworld$limitInput.isFocused() && limit != this.chaosworld$limit) {
                this.chaosworld$limit = limit;
                this.chaosworld$limitInput.setValue(String.valueOf(limit));
            }
            this.chaosworld$limitInput.setX(this.chaosworld$smartToggle.getX() - this.chaosworld$limitInput.getWidth() - 6);
            this.chaosworld$limitInput.setY(this.chaosworld$smartToggle.getY());
        } else {
            this.removeWidget(this.chaosworld$limitInput);
        }
    }
}
