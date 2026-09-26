package com.yongaishide.chaosworld.patch.ufo;

import java.util.Set;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Patch layer: removes UFO Future's mega crafting storage / mega co-processor
 * content (blocks, components and their recipes), matching the Chaos World
 * "UFO Future reset" content removal. UFO storage cells and cell housings are
 * kept enabled.
 */
public final class UfoContentRemoval {

    private UfoContentRemoval() {
    }

    public static final Set<String> REMOVED_ITEM_PATHS = Set.of(
            "1b_mega_crafting_storage",
            "50b_mega_crafting_storage",
            "1t_mega_crafting_storage",
            "250t_mega_crafting_storage",
            "1qd_mega_crafting_storage",
            "50m_mega_co_processor",
            "150m_mega_co_processor",
            "300m_mega_co_processor",
            "750m_mega_co_processor",
            "2b_mega_co_processor",
            // UFO Future v3 re-added infinite / mega endgame content
            "infinity_fabrication_singularity_controller",
            "quantum_computation_nexus_controller",
            "quantum_pattern_fabrication_matrix_controller",
            "quantum_pattern_buffer",
            "quantum_pattern_proxy",
            "quantum_interface",
            "quantum_grid_link"
    );

    public static final Set<String> REMOVED_RECIPE_PATHS = Set.of(
            "coprocessor_50m",
            "coprocessor_150m",
            "coprocessor_300m",
            "coprocessor_750m",
            "coprocessor_2b",
            "storage_1b",
            "storage_50b",
            "storage_1t",
            "storage_250t",
            "storage_1qd",
            "entropic_convergence_casing",
            "entropic_convergence_engine"
    );

    public static boolean isRemovedItemId(@org.jetbrains.annotations.Nullable ResourceLocation id) {
        if (id == null || !"ufo".equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        return REMOVED_ITEM_PATHS.contains(path)
                || path.endsWith("_mega_crafting_storage")
                || path.endsWith("_mega_co_processor");
    }

    public static boolean isRemovedStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return isRemovedItemId(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isRemovedRecipe(ResourceLocation id, JsonElement json) {
        if ("ufo".equals(id.getNamespace()) && REMOVED_RECIPE_PATHS.contains(id.getPath())) {
            return true;
        }
        String text = json.toString();
        for (String path : REMOVED_ITEM_PATHS) {
            if (text.contains("ufo:" + path)) {
                return true;
            }
        }
        return false;
    }
}
