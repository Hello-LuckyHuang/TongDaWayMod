package com.helloluckyhuang.tongdaway.util;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.way.RailwayMap;
import com.helloluckyhuang.tongdaway.way.RegionPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SaveWayData {
    public static final LevelResource WAY_DATA_DIR = new LevelResource("tongdaway_data");
    public static final String NAME = "tongdaway_mod_way_data";

    public static void putRailwayMap(RegionPos regionPos, RailwayMap railwayMap, MinecraftServer server) {
        try {
            Path path = server.getWorldPath(WAY_DATA_DIR);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            File file = new File(path + "/" + NAME + "_" + regionPos + ".nbt");
            if (!file.exists()) {
                file.createNewFile();
            }
            boolean created = file.exists();
            if (created) {
                OutputStream resourceStream = new FileOutputStream(file);
                try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(
                        new GZIPOutputStream(resourceStream)))) {
                    NbtIo.write(railwayMap.toNBT(), stream);
                } catch (Exception ex) {
                    TongDaWay.LOGGER.error("Error saving data for region {}", regionPos, ex);
                }
            }
        } catch (Exception ex) {
            TongDaWay.LOGGER.error("Error saving data for region {}", regionPos, ex);
        }
    }

    public static RailwayMap getRailwayMap(RegionPos regionPos, MinecraftServer server) {
        try {
            Path path = server.getWorldPath(WAY_DATA_DIR);
            File file = new File(path + "/" + NAME + "_" + regionPos + ".nbt");
            if (!file.exists())
                return null;
            InputStream resourceStream = new FileInputStream(file);
            try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                    new GZIPInputStream(resourceStream)))) {
                CompoundTag rootTag = NbtIo.read(stream, NbtAccounter.create(0x20000000L));
                return RailwayMap.fromNBT(rootTag);
            } catch (Exception ex) {
                TongDaWay.LOGGER.error("Error loading data for region {}", regionPos, ex);
            }
        } catch (Exception ex) {
            TongDaWay.LOGGER.error("Error loading data for region {}", regionPos, ex);
        }

        return null;
    }
}
