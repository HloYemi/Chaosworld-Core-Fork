package com.yongaishide.chaosworld.mixin.ufo.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.raishxn.ufo.client.GhostHologramRenderer;
import com.yongaishide.chaosworld.patch.contents.TintedVertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Patch layer: ghost hologram blocks are drawn with a stronger translucent tint. */
@Mixin(value = GhostHologramRenderer.class, remap = false)
public abstract class GhostHologramRendererMixin {

    @Redirect(method = "onRenderLevelStage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private static VertexConsumer chaosworld$tintHologram(MultiBufferSource.BufferSource bufferSource,
            RenderType renderType) {
        return new TintedVertexConsumer(bufferSource.getBuffer(renderType), 90);
    }
}
