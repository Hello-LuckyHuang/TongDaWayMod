package com.helloluckyhuang.tongdaway.structure;

import com.google.gson.*;
import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.util.MyRandom;
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
    public static final Map<Integer, CrossTemplate> normalCross = new HashMap<>();
    // 地下路口
    public static final Map<Integer, CrossTemplate> undergroundCross = new HashMap<>();

    // 路面路基
    public static final Map<Integer, RoadTemplate> ground = new HashMap<>();
    // 隧道
    public static final Map<Integer, RoadTemplate> tunnel = new HashMap<>();
    // 桥梁
    public static final Map<Integer, BridgeTemplate> bridge = new HashMap<>();

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
                String temType = GsonHelper.getAsString(jsonobject, "class");
                String type = GsonHelper.getAsString(jsonobject, "type");
                String nbt = GsonHelper.getAsString(jsonobject, "template");
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
                    int id = location.getPath().hashCode();
                    if (temType.equals("cross")) {
                        switch (type) {
                            case "normal" -> {
                                CrossTemplate crossTemplate = new CrossTemplate(rootTag, id, CrossTemplate.StationType.NORMAL);
                                normalCross.put(id, crossTemplate);
                            }
                            case "underground" -> {
                                CrossTemplate crossTemplate = new CrossTemplate(rootTag, id, CrossTemplate.StationType.UNDER_GROUND);
                                undergroundCross.put(id, crossTemplate);
                            }
                        }

                    } else if (temType.equals("road")) {
                        switch (type) {
                            case "ground" -> {
                                RoadTemplate roadTemplate = new RoadTemplate(rootTag);
                                ground.put(id, roadTemplate);
                            }
                            case "tunnel" -> {
                                RoadTemplate roadTemplate = new RoadTemplate(rootTag);
                                tunnel.put(id, roadTemplate);
                            }
                            case "bridge" -> {
                                int deckStart = GsonHelper.getAsInt(jsonobject, "deck_start");
                                int deckEnd = GsonHelper.getAsInt(jsonobject, "deck_end");
                                int heightOffset = GsonHelper.getAsInt(jsonobject, "height_offset");
                                BridgeTemplate bridgeTemplate = new BridgeTemplate(rootTag, deckStart, deckEnd, heightOffset);
                                bridge.put(id, bridgeTemplate);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                TongDaWay.LOGGER.error("Load Structure Data Err: {}", location.getPath(), e);
            }
        });
    }

    // 随机获取用于生成的结构模板
    public static CrossTemplate getRandomNormalCross(long seed) {
        if (normalCross.isEmpty()) {
            return null;
        }

        return MyRandom.getRandomValueFromMap(normalCross, 84_269 + seed*10000);
    }

    public static CrossTemplate getRandomUnderGroundCross(long seed) {
        if (undergroundCross.isEmpty()) {
            return null;
        }

        return MyRandom.getRandomValueFromMap(undergroundCross, 71_1551 + seed*10000);
    }

    public static RoadTemplate getRandomGround(long seed) {
        if (ground.isEmpty()) {
            return null;
        }

        return MyRandom.getRandomValueFromMap(ground, 84_270 + seed*10000);
    }

    public static RoadTemplate getRandomTunnel(long seed) {
        if (tunnel.isEmpty()) {
            return null;
        }

        return MyRandom.getRandomValueFromMap(tunnel, 71_1553 + seed*10000);
    }

    public static BridgeTemplate getRandomBridge(long seed) {
        if (bridge.isEmpty()) {
            return null;
        }

        return MyRandom.getRandomValueFromMap(bridge, 90_318 + seed*10000);
    }
}
