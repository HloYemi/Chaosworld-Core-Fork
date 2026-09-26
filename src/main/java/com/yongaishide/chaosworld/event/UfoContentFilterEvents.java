package com.yongaishide.chaosworld.event;

import java.util.ArrayList;
import java.util.List;

import com.yongaishide.chaosworld.ChaosWorld;
import com.yongaishide.chaosworld.patch.ufo.UfoContentRemoval;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Patch layer: hides the removed UFO Future content (infinite / mega storage and
 * co-processor blocks) from every creative tab.
 */
@EventBusSubscriber(modid = ChaosWorld.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class UfoContentFilterEvents {

    private UfoContentFilterEvents() {
    }

    @SubscribeEvent
    public static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> toRemove = new ArrayList<>();
        for (ItemStack stack : event.getParentEntries()) {
            if (UfoContentRemoval.isRemovedStack(stack)) {
                toRemove.add(stack);
            }
        }
        for (ItemStack stack : event.getSearchEntries()) {
            if (UfoContentRemoval.isRemovedStack(stack) && !toRemove.contains(stack)) {
                toRemove.add(stack);
            }
        }
        for (ItemStack stack : toRemove) {
            event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
