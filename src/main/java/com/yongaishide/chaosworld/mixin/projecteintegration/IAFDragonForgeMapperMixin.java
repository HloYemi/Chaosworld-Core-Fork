package com.yongaishide.chaosworld.mixin.projecteintegration;

import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.tagnumelite.projecteintegration.addons.IceAndFireCEAddon$IAFDragonForgeMapper", remap = false)
public class IAFDragonForgeMapperMixin {

    @Inject(method = "canHandle", at = @At("HEAD"), cancellable = true, remap = false)
    private void chaosworld$skipBrokenIceAndFireMapper(RecipeType<?> recipeType, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
