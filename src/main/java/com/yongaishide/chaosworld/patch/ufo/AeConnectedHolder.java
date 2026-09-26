package com.yongaishide.chaosworld.patch.ufo;

/**
 * Shared state bridge for the "AE connected" GUI flag between the simple and
 * parallel multiblock controller mixins (a mixin cannot @Shadow a field that is
 * added by another mixin).
 */
public interface AeConnectedHolder {

    boolean chaosworld$isAeConnected();

    void chaosworld$setAeConnected(boolean value);
}
