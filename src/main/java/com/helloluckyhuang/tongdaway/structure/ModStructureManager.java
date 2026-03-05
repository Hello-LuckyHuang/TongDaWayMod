package com.helloluckyhuang.tongdaway.structure;

import com.google.gson.*;
import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.util.RandomPool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

@EventBusSubscriber
public class ModStructureManager extends SimpleJsonResourceReloadListener<JsonElement> {
    private static final String folder = "way_structure";

    // 路口
    public static final RandomPool<CrossTemplate> cross = new RandomPool<>();

    // 路上地物
    public static final RandomPool<RoadFeatureTemplate> roadFeature = new RandomPool<>();

    // 路基
    public static final RandomPool<RoadTemplate> roadbed = new RandomPool<>();

    // 桥梁
    public static final RandomPool<BridgeTemplate> bridge = new RandomPool<>();

    @SubscribeEvent
    public static void addReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(ResourceLocation.fromNamespaceAndPath(TongDaWay.MODID, folder), new ModStructureManager());
    }

    public ModStructureManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(folder));
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<ResourceLocation, JsonElement> jsonData = new HashMap<>();
        resourceManager.listResources(folder, location ->
                location.getPath().endsWith(".json")
        ).forEach((location, resource) -> {
            try {
                Gson gson = new Gson();
                InputStream resourceStream = resourceManager
                    .getResource(location)
                    .orElseThrow()
                    .open();
                InputStreamReader reader = new InputStreamReader(resourceStream);
                jsonData.put(location, gson.fromJson(reader, JsonElement.class));
            } catch (IOException e) {
                TongDaWay.LOGGER.error("Error loading structure json file: ", e);
            }
        });
        return jsonData;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceList, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        resourceList.forEach((location, json) -> {
            try {
                JsonObject jsonobject = json.getAsJsonObject();
                String temClass = GsonHelper.getAsString(jsonobject, "class");
                String type = GsonHelper.getAsString(jsonobject, "type");
                String nbt = GsonHelper.getAsString(jsonobject, "template");
                int heightOffset = GsonHelper.getAsInt(jsonobject, "height_offset");
                List<JsonElement> tags = GsonHelper.getAsJsonArray(jsonobject, "tags").asList();
                ResourceLocation nbtLocation = ResourceLocation.fromNamespaceAndPath(
                        nbt.split(":")[0],
                        "structure/" + nbt.split(":")[1] + ".nbt"
                );
                CompoundTag rootTag = null;
                InputStream resourceStream = resourceManagerIn
                        .getResource(nbtLocation)
                        .orElseThrow()
                        .open();
                try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                        new GZIPInputStream(resourceStream)))) {
                    rootTag = NbtIo.read(stream, NbtAccounter.create(0x20000000L));
                } catch (Exception e) {
                    TongDaWay.LOGGER.error("Load Structure nbt file Err: {}", nbtLocation.getPath(), e);
                }

                if (rootTag != null) {
                    // 计算结构id
                    int id = location.getPath().hashCode();
                    // 获得标签数组(若为空,则默认添加"default"标签)
                    int length = tags.isEmpty() ? 2 : tags.size()+1;
                    String[] tagArray = new String[length];
                    tagArray[0] = type;
                    if (tags.isEmpty())
                        tagArray[1] = "default";
                    else
                        for (int i = 0; i < tags.size(); i++) {
                            tagArray[i+1] = tags.get(i).getAsString();
                        }
                    // 加入结构池
                    switch (temClass) {
                        case "cross" -> {
                            CrossTemplate crossTemplate = new CrossTemplate(rootTag, id, heightOffset);
                            cross.add(crossTemplate, id, tagArray);
                        }
                        case "road" -> {
                            if (type.equals("bridge")) {
                                // 桥梁单列处理
                                int deckStart = GsonHelper.getAsInt(jsonobject, "deck_start");
                                int deckEnd = GsonHelper.getAsInt(jsonobject, "deck_end");
                                BridgeTemplate bridgeTemplate = new BridgeTemplate(rootTag, deckStart, deckEnd, heightOffset);
                                bridge.add(bridgeTemplate, id, tagArray);
                            } else {
                                RoadTemplate roadTemplate = new RoadTemplate(rootTag, heightOffset);
                                roadbed.add(roadTemplate, id, tagArray);
                            }
                        }
                        case "road_feature" -> {
                            RoadFeatureTemplate roadFeatureTemplate = new RoadFeatureTemplate(rootTag, heightOffset);
                            roadFeature.add(roadFeatureTemplate, id, tagArray);
                        }
                    }
                }
            } catch (Exception e) {
                TongDaWay.LOGGER.error("Load Structure Data Err: {}", location.getPath(), e);
            }
        });
    }

    // 随机获取用于生成的结构模板
    public static CrossTemplate getRandomNormalCross(long seed, String... tags) {
        String type = "normal";
        return cross.get(84_269 + seed*10000, type, tags);

//     System.arraycopy(tags, 0, tagArray, 1, tags.length);
    }

    public static CrossTemplate getRandomUnderGroundCross(long seed, String... tags) {
        String type = "underground";
        return cross.get(71_1552 + seed*10000, type, tags);
    }

    public static RoadTemplate getRandomGround(long seed, String... tags) {
        String type = "ground";
        return roadbed.get(84_270 + seed*10000, type, tags);
    }

    public static RoadTemplate getRandomTunnel(long seed, String... tags) {
        String type = "tunnel";
        return roadbed.get(71_1553 + seed*10000, type, tags);
    }

    public static RoadTemplate getRandomShortBridge(long seed, String... tags) {
        String type = "short_bridge";
        return roadbed.get(71_1553 + seed*10000, type, tags);
    }

    public static BridgeTemplate getRandomBridge(long seed, String... tags) {
        String type = "bridge";
        return bridge.get(90_318 + seed*10000, type, tags);
    }
}
