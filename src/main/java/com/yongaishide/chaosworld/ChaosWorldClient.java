package com.yongaishide.chaosworld;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side registrations owned by Chaos World Core.
 * UFO Future registers its own screens through its own client setup.
 */
public class ChaosWorldClient {

    public ChaosWorldClient(IEventBus eventBus) {
        eventBus.addListener(this::registerScreens);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(com.yongaishide.chaosworld.mekanism.MekanismMachines.MECHANICAL_ASSEMBLER_CONTAINER.get(),
                com.yongaishide.chaosworld.mekanism.client.gui.GuiMechanicalAssembler::new);
        event.register(com.yongaishide.chaosworld.mekanism.DragonSoulForgeMachines.DRAGON_SOUL_FORGE_CONTAINER.get(),
                com.yongaishide.chaosworld.mekanism.client.gui.GuiDragonSoulForge::new);
        event.register(com.yongaishide.chaosworld.mekanism.vein.VeinDrillMachines.VEIN_DRILL_CONTAINER.get(),
                com.yongaishide.chaosworld.mekanism.vein.client.gui.GuiVeinDrill::new);
    }
}
