package com.yongaishide.chaosworld.mekanism.vein;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.yongaishide.chaosworld.ChaosWorld;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

/**
 * 虚脉钻探机的数据包配置:
 * - ore_pools/*.json:统一矿石池(物品/流体/气体条目,维度已合并)
 * - vein_sizes.json:矿脉大小等级的质量范围
 */
@EventBusSubscriber(modid = ChaosWorld.MODID)
public class VeinDrillDataLoader {

    private static final Logger LOGGER = LogUtils.getLogger();

    //统一矿石池:所有 ore_pools/*.json 的条目
    private static final List<OreEntry> ORE_ENTRIES = new java.util.concurrent.CopyOnWriteArrayList<>();
    //修复品:物品 id -> 修复量(数据包配置,默认铁系)
    private static final Map<ResourceLocation, Integer> REPAIR_ITEMS = new ConcurrentHashMap<>();
    //大小等级 -> [min, max](数据包覆盖,默认值兜底)
    private static final long[][] DEFAULT_SIZES = {
          {50_000, 250_000},
          {250_000, 2_500_000},
          {2_500_000, 37_500_000},
          {37_500_000, 750_000_000},
          {750_000_000, 18_750_000_000L},
          {18_750_000_000L, 562_500_000_000L},
          {562_500_000_000L, 19_687_500_000_000L}
    };
    private static volatile long[][] SIZE_RANGES = DEFAULT_SIZES;

    private VeinDrillDataLoader() {
    }

    public static void init(AddReloadListenerEvent event) {
        //标准做法:第二参为数据包相对 data/<modid> 的目录路径
        event.addListener(new SimpleJsonResourceReloadListener(new com.google.gson.Gson(), "vein_drill") {
            @Override
            protected void apply(Map<ResourceLocation, com.google.gson.JsonElement> data, ResourceManager resourceManager,
                  ProfilerFiller profiler) {
                ORE_ENTRIES.clear();
                REPAIR_ITEMS.clear();
                List<ResourceLocation> listed = new ArrayList<>();
                for (Map.Entry<ResourceLocation, com.google.gson.JsonElement> entry : data.entrySet()) {
                    ResourceLocation id = entry.getKey();
                    if (id.getNamespace().equals(ChaosWorld.MODID) && id.getPath().startsWith("ore_pools/")) {
                        try {
                            JsonObject obj = entry.getValue().getAsJsonObject();
                            for (com.google.gson.JsonElement e : obj.getAsJsonArray("entries")) {
                                ORE_ENTRIES.add(OreEntry.parse(e.getAsJsonObject()));
                            }
                            listed.add(id);
                        } catch (Exception ex) {
                            LOGGER.error("Failed to load vein drill ore pool {}, error: {}", id, ex.toString());
                        }
                    } else if (id.getNamespace().equals(ChaosWorld.MODID) && id.getPath().equals("vein_sizes")) {
                        try {
                            JsonObject obj = entry.getValue().getAsJsonObject();
                            com.google.gson.JsonArray arr = obj.getAsJsonArray("sizes");
                            long[][] ranges = new long[VeinSize.values().length][];
                            for (int i = 0; i < arr.size() && i < ranges.length; i++) {
                                JsonObject s = arr.get(i).getAsJsonObject();
                                ranges[i] = new long[]{s.get("min").getAsLong(), s.get("max").getAsLong()};
                            }
                            for (int i = 0; i < ranges.length; i++) {
                                if (ranges[i] == null) {
                                    ranges[i] = DEFAULT_SIZES[i];
                                }
                            }
                            SIZE_RANGES = ranges;
                            LOGGER.info("Loaded {} vein sizes", arr.size());
                        } catch (Exception ex) {
                            LOGGER.error("Failed to load vein sizes, error: {}", ex.toString());
                        }
                    } else if (id.getNamespace().equals(ChaosWorld.MODID) && id.getPath().equals("repair_items")) {
                        try {
                            JsonObject obj = entry.getValue().getAsJsonObject();
                            for (com.google.gson.JsonElement e : obj.getAsJsonArray("items")) {
                                JsonObject itemObj = e.getAsJsonObject();
                                ResourceLocation itemId = ResourceLocation.parse(itemObj.get("id").getAsString());
                                int amount = itemObj.has("repair") ? itemObj.get("repair").getAsInt() : 25;
                                REPAIR_ITEMS.put(itemId, amount);
                            }
                        } catch (Exception ex) {
                            LOGGER.error("Failed to load vein drill repair items, error: {}", ex.toString());
                        }
                    }
                }
                LOGGER.info("Loaded {} vein drill ore pools, {} repair items", listed.size(), REPAIR_ITEMS.size());
            }
        });
    }

    /**
     * 修复品数据:物品 id -> 修复量(空则用默认铁系)。
     */
    public static Map<ResourceLocation, Integer> getRepairItems() {
        return REPAIR_ITEMS;
    }

    /**
     * 统一矿石池:合并所有 ore_pools 文件中的条目(重复条目权重累加),
     * 产出只与钻头等级相关。
     */
    public static List<OreEntry> getUnifiedPool() {
        java.util.Map<OreEntry, Integer> merged = new java.util.LinkedHashMap<>();
        for (OreEntry entry : ORE_ENTRIES) {
            merged.merge(entry, entry.weight(), Integer::sum);
        }
        return new ArrayList<>(merged.keySet());
    }

    /**
     * 矿脉大小等级的质量范围(数据包覆盖,默认值兜底)。
     */
    public static long[] getSizeRange(VeinSize size) {
        return SIZE_RANGES[size.getId()];
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        init(event);
    }
}
