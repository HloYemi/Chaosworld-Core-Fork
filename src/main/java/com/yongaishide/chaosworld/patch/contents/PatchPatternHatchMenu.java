package com.yongaishide.chaosworld.patch.contents;

import appeng.api.config.YesNo;

/**
 * Bridge for the extra ExtendedAE+ pattern provider settings added to the
 * Quantum Pattern Hatch menu by the patch layer.
 */
public interface PatchPatternHatchMenu {

    YesNo chaosworld$getAdvancedBlocking();

    YesNo chaosworld$getSmartDoubling();

    int chaosworld$getPerProviderScalingLimit();

    void chaosworld$sendScalingLimitFromClient(int limit);
}
