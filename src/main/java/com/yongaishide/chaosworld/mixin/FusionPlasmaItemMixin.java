package com.yongaishide.chaosworld.mixin;

import com.yongaishide.chaosworld.init.ModRecipes;
import com.yongaishide.chaosworld.recipe.FusionConversionRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Data-driven conversion performed when a matching item is dropped into the
 * plasma of a burning Mekanism fusion reactor. The recipe set is read from the
 * {@code chaosworld_core:fusion_conversion} recipe type, so any data pack (such
 * as KubeJS) can define additional conversions without touching the mod.
 * <p>
 * Runs on the always-present ItemEntity. Mekanism Generators classes are only
 * touched via reflection, so the mod loads fine without Mekanism Generators.
 */
@Mixin(ItemEntity.class)
public abstract class FusionPlasmaItemMixin {

    private static final String CONTROLLER_CLASS = "mekanism.generators.common.tile.fusion.TileEntityFusionReactorController";

    @Inject(method = "tick", at = @At("HEAD"))
    private void ufo$convertInPlasma(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide || self.tickCount % 10 != 0) {
            return;
        }
        FusionConversionRecipe recipe = findRecipe(level, self.getItem());
        if (recipe == null) {
            return;
        }
        if (!net.neoforged.fml.ModList.get().isLoaded("mekanismgenerators")) {
            return;
        }
        try {
            convertInFusionReactor(self, recipe);
        } catch (Throwable ignored) {
            // Never let a compat issue break item ticking
        }
    }

    private static FusionConversionRecipe findRecipe(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (RecipeHolder<FusionConversionRecipe> holder : level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.FUSION_CONVERSION_TYPE.get())) {
            FusionConversionRecipe recipe = holder.value();
            if (recipe.getInput().test(stack)) {
                return recipe;
            }
        }
        return null;
    }

    private static void convertInFusionReactor(ItemEntity item, FusionConversionRecipe recipe) throws Exception {
        Level level = item.level();
        BlockPos pos = item.blockPosition();
        Class<?> controllerClass = Class.forName(CONTROLLER_CLASS);
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))) {
            BlockEntity be = level.getBlockEntity(check);
            if (be == null || !controllerClass.isInstance(be)) {
                continue;
            }
            Object data = controllerClass.getMethod("getMultiblock").invoke(be);
            if (data == null) {
                continue;
            }
            if (!(boolean) data.getClass().getMethod("isFormed").invoke(data)) {
                continue;
            }
            if (!(boolean) data.getClass().getMethod("isBurning").invoke(data)) {
                continue;
            }
            BlockPos min = (BlockPos) data.getClass().getMethod("getMinPos").invoke(data);
            BlockPos max = (BlockPos) data.getClass().getMethod("getMaxPos").invoke(data);
            AABB interior = AABB.encapsulatingFullBlocks(min.offset(1, 1, 1), max.offset(-1, -1, -1));
            if (!interior.contains(item.getX(), item.getY(), item.getZ())) {
                continue;
            }

            ItemStack stack = item.getItem();
            ItemStack result = recipe.getResult();
            int total = Math.min(result.getCount() * stack.getCount(), result.getMaxStackSize());
            item.setItem(result.copyWithCount(Math.max(1, total)));
            // Eject the result out of the plasma (the reactor's interior is lethal)
            item.setDeltaMovement(0, 0.4, 0);
            item.setPos(item.getX(), interior.maxY + 0.5, item.getZ());
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                        item.getX(), item.getY() - 1.0, item.getZ(), 24, 0.5, 0.5, 0.5, 0.06);
                serverLevel.playSound(null, item.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.7F);
            }
            return;
        }
    }
}
