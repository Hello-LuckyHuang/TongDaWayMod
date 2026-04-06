package com.helloluckyhuang.tongdaway.util;

import com.helloluckyhuang.tongdaway.Tongdaway;
import com.helloluckyhuang.tongdaway.way.WayMap;
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

    public static void putWayMap(RegionPos regionPos, WayMap wayMap, MinecraftServer server) {
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
                    NbtIo.write(wayMap.toNBT(), stream);
                } catch (Exception ex) {
                    Tongdaway.LOGGER.error("Error saving data for region {}", regionPos, ex);
                }
            }
        } catch (Exception ex) {
            Tongdaway.LOGGER.error("Error saving data for region {}", regionPos, ex);
        }
    }

    public static WayMap getWayMap(RegionPos regionPos, MinecraftServer server) {
        try {
            Path path = server.getWorldPath(WAY_DATA_DIR);
            File file = new File(path + "/" + NAME + "_" + regionPos + ".nbt");
            if (!file.exists())
                return null;
            InputStream resourceStream = new FileInputStream(file);
            try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                    new GZIPInputStream(resourceStream)))) {
                CompoundTag rootTag = NbtIo.read(stream, new NbtAccounter(0x20000000L));
                return WayMap.fromNBT(rootTag);
            } catch (Exception ex) {
                Tongdaway.LOGGER.error("Error loading data for region {}", regionPos, ex);
            }
        } catch (Exception ex) {
            Tongdaway.LOGGER.error("Error loading data for region {}", regionPos, ex);
        }

        return null;
    }
}
