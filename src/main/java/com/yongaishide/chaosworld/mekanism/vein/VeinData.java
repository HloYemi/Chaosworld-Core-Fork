package com.yongaishide.chaosworld.mekanism.vein;

import java.util.concurrent.ThreadLocalRandom;
import com.yongaishide.chaosworld.mekanism.vein.OreEntry.PoolType;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * 虚拟矿脉数据:大小等级(决定容量范围)+ 质量剩余。
 * 通过 NBT 挂在虚拟矿脉核心物品上;钻探机内部维护一份当前矿脉。
 */
public record VeinData(VeinSize size, long massCapacity, long massRemaining) {

    private static final String KEY_SIZE = "veinSize";
    private static final String KEY_CAPACITY = "veinCapacity";
    private static final String KEY_REMAINING = "veinRemaining";

    /**
     * 按维度 + 大小等级随机质量,生成一个矿脉(容量在等级范围内随机)。
     */
    public static VeinData generate(Level level) {
        return generate(level, 0);
    }

    /**
     * 按维度 + 大小等级随机质量,生成一个矿脉;minLevel 为保底等级(独立升级)。
     */
    public static VeinData generate(Level level, int minLevel) {
        VeinSize size = randomSize(minLevel);
        long[] range = VeinDrillDataLoader.getSizeRange(size);
        long capacity = ThreadLocalRandom.current().nextLong(range[0], range[1] + 1);
        return new VeinData(size, capacity, capacity);
    }

    private static VeinSize randomSize(int minLevel) {
        int length = VeinSize.values().length;
        int id = minLevel >= length - 1 ? length - 1 : ThreadLocalRandom.current().nextInt(minLevel, length);
        return VeinSize.byId(id);
    }

    public VeinData consumeMass(long amount) {
        return new VeinData(size, massCapacity, Math.max(0, massRemaining - amount));
    }

    public boolean isEmpty() {
        return massRemaining <= 0;
    }

    public String sizeName() {
        return "vein.size." + size.getName();
    }

    public CompoundTag write(CompoundTag tag) {
        tag.putByte(KEY_SIZE, (byte) size.getId());
        tag.putLong(KEY_CAPACITY, massCapacity);
        tag.putLong(KEY_REMAINING, massRemaining);
        return tag;
    }

    @Nullable
    public static VeinData read(CompoundTag tag) {
        if (!tag.contains(KEY_SIZE) || !tag.contains(KEY_CAPACITY)) {
            return null;
        }
        return new VeinData(VeinSize.byId(tag.getByte(KEY_SIZE)), tag.getLong(KEY_CAPACITY), tag.getLong(KEY_REMAINING));
    }

    /**
     * 虚拟矿脉核心物品的 NBT 读写助手。
     */
    public static class VeinNbt {

        private static final String KEY = "veinData";

        public static ItemStack write(ItemStack stack, VeinData data) {
            CompoundTag compound = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                  net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            compound.put(KEY, data.write(new CompoundTag()));
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(compound));
            return stack;
        }

        @Nullable
        public static VeinData read(ItemStack stack) {
            if (stack.isEmpty()) {
                return null;
            }
            net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (customData == null || !customData.getUnsafe().contains(KEY)) {
                return null;
            }
            return VeinData.read(customData.getUnsafe().getCompound(KEY));
        }
    }

    /**
     * 概率选取池中一个条目(按权重;池空返回 null)。
     */
    @Nullable
    public static OreEntry rollEntry(java.util.List<OreEntry> pool) {
        if (pool.isEmpty()) {
            return null;
        }
        int total = 0;
        for (OreEntry entry : pool) {
            total += entry.weight();
        }
        if (total <= 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (OreEntry entry : pool) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry;
            }
        }
        return pool.get(pool.size() - 1);
    }

    /**
     * 将矿石条目转换成可插入输出槽的物品堆叠。
     * ITEM 直接物品;FLUID 转对应桶(若无桶则空桶);GAS 转 mekanism 气体罐(若无则空气罐)。
     */
    public static ItemStack itemStackFor(OreEntry entry, Level level) {
        return switch (entry.type()) {
            case ITEM -> new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.id())));
            case FLUID -> {
                var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(entry.id()));
                yield fluid.getBucket() == null || fluid.getBucket() == net.minecraft.world.item.Items.AIR
                      ? new ItemStack(net.minecraft.world.item.Items.BUCKET)
                      : new ItemStack(fluid.getBucket());
            }
            case GAS -> new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:gas_canister")));
        };
    }

    /**
     * 将矿石条目转换成对应堆叠/罐内容(旧接口)。
     */
    public static Material fromEntry(OreEntry entry, Level level) {
        return switch (entry.type()) {
            case ITEM -> Material.item(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.id()))));
            case FLUID -> Material.fluid(new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse(entry.id())), 1000));
            case GAS -> Material.gas(new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.get(ResourceLocation.parse(entry.id())), 1000));
        };
    }

    /**
     * 产物载体:物品/流体/气体任一片(兼容旧接口)。
     */
    public record Material(ItemStack itemStack, @Nullable FluidStack fluidStack, @Nullable ChemicalStack chemicalStack) {

        public static Material item(ItemStack stack) {
            return new Material(stack, null, null);
        }

        public static Material fluid(FluidStack stack) {
            return new Material(ItemStack.EMPTY, stack, null);
        }

        public static Material gas(ChemicalStack stack) {
            return new Material(ItemStack.EMPTY, null, stack);
        }
    }
}
