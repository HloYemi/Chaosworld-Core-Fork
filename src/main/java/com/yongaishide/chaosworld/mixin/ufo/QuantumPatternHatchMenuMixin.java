package com.yongaishide.chaosworld.mixin.ufo;

import appeng.api.config.YesNo;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import com.extendedae_plus.api.config.EAPSettings;
import com.extendedae_plus.api.smartDoubling.ISmartDoublingHolder;
import com.raishxn.ufo.screen.QuantumPatternHatchMenu;
import com.yongaishide.chaosworld.patch.contents.PatchPatternHatchMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Patch layer: ExtendedAE+ advanced blocking / smart doubling settings on the
 * Quantum Pattern Hatch menu (from the Chaos World "UFO Future reset").
 */
@Mixin(value = QuantumPatternHatchMenu.class, remap = false)
public abstract class QuantumPatternHatchMenuMixin extends AEBaseMenu implements PatchPatternHatchMenu {

    @Unique
    private static final String CHAOSWORLD$ACTION_SET_LIMIT = "chaosworld$setPerProviderScalingLimit";

    @Shadow(remap = false)
    protected PatternProviderLogic logic;

    @GuiSync(8)
    public YesNo chaosworld$advancedBlocking = YesNo.NO;

    @GuiSync(9)
    public YesNo chaosworld$smartDoubling = YesNo.NO;

    @GuiSync(10)
    public int chaosworld$perProviderScalingLimit = 0;

    public QuantumPatternHatchMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void chaosworld$registerActions(MenuType<?> menuType, int id, Inventory playerInventory,
            PatternProviderLogicHost host, CallbackInfo ci) {
        this.registerClientAction(CHAOSWORLD$ACTION_SET_LIMIT, Integer.class, this::chaosworld$handleSetLimit);
    }

    @Inject(method = "broadcastChanges", at = @At("TAIL"), remap = false)
    private void chaosworld$refresh(CallbackInfo ci) {
        if (this.logic == null) {
            return;
        }
        this.chaosworld$advancedBlocking = this.logic.getConfigManager().getSetting(EAPSettings.ADVANCED_BLOCKING);
        this.chaosworld$smartDoubling = this.logic.getConfigManager().getSetting(EAPSettings.SMART_DOUBLING);
        this.chaosworld$perProviderScalingLimit = this.logic instanceof ISmartDoublingHolder holder
                ? holder.eap$getProviderSmartDoublingLimit()
                : 0;
    }

    @Unique
    private void chaosworld$handleSetLimit(int limit) {
        if (this.logic instanceof ISmartDoublingHolder holder) {
            holder.eap$setProviderSmartDoublingLimit(limit);
            this.logic.saveChanges();
        }
    }

    @Override
    public YesNo chaosworld$getAdvancedBlocking() {
        return this.chaosworld$advancedBlocking;
    }

    @Override
    public YesNo chaosworld$getSmartDoubling() {
        return this.chaosworld$smartDoubling;
    }

    @Override
    public int chaosworld$getPerProviderScalingLimit() {
        return this.chaosworld$perProviderScalingLimit;
    }

    @Override
    public void chaosworld$sendScalingLimitFromClient(int limit) {
        if (this.isClientSide()) {
            this.sendClientAction(CHAOSWORLD$ACTION_SET_LIMIT, limit);
        }
    }
}
