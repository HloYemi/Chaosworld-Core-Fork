package com.yongaishide.chaosworld.mixin;

import java.util.Map;

import com.google.gson.JsonElement;
import com.yongaishide.chaosworld.patch.ufo.UfoContentRemoval;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Patch layer: drops the recipes of UFO Future's removed infinite / mega content
 * (mega crafting storages, mega co-processors, infinity component and the
 * entropic convergence engine) before they are parsed.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
    private void chaosworld$removeUfoContent(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager,
            ProfilerFiller profiler, CallbackInfo ci) {
        map.entrySet().removeIf(entry -> UfoContentRemoval.isRemovedRecipe(entry.getKey(), entry.getValue()));
    }
}
