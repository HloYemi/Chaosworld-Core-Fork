package com.yongaishide.chaosworld.patch.contents;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Vertex consumer wrapper that scales vertex alpha (ghost hologram tint). */
public final class TintedVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final int alpha;

    public TintedVertexConsumer(VertexConsumer delegate, int alpha) {
        this.delegate = delegate;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        return delegate.addVertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int a) {
        return delegate.setColor(red, green, blue, a * alpha / 255);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return delegate.setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return delegate.setUv1(u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return delegate.setUv2(u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return delegate.setNormal(x, y, z);
    }
}
