package com.yongaishide.chaosworld.mekanism;

import mekanism.api.text.ILangEntry;

/**
 * 机械组装机的描述文本,翻译键:description.chaosworld_core.mechanical_assembler
 */
public class MechanicalAssemblerLang implements ILangEntry {

    private final String translationKey;

    public MechanicalAssemblerLang(String path) {
        this.translationKey = "description.chaosworld_core." + path;
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }
}
