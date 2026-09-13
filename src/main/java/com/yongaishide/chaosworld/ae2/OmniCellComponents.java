package com.yongaishide.chaosworld.ae2;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ae2omnicells 全能存储组件的判定与容量解析。
 * <p>
 * 组件命名如 {@code omni_cell_component_256k}，
 * 仅接受 k 级别组件(k = KiB,1k 组件 = 1024 字节)；m 级别组件容量过大，
 * 会在物质聚合器中触发原版 {@code fillOutput()} 的循环失控进而卡死，故不支持。
 * 兼容 omni / complex_omni / quantum_omni 三系。
 */
public final class OmniCellComponents {

    private static final Pattern PATH = Pattern.compile("^.*_cell_component_(\\d+)k$");
    private static final long KB = 1024L;

    private OmniCellComponents() {
    }

    public static boolean isOmniCellComponent(ItemStack stack) {
        return bytesOf(stack) > 0;
    }

    public static long bytesOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        var loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"ae2omnicells".equals(loc.getNamespace())) {
            return 0;
        }
        Matcher m = PATH.matcher(loc.getPath());
        if (!m.matches()) {
            return 0;
        }
        long value = Long.parseLong(m.group(1));
        return value * KB;
    }
}
