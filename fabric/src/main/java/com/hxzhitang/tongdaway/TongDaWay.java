package com.hxzhitang.tongdaway;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hxzhitang.tongdaway.util.blocks.ModBlockEntities;
import com.hxzhitang.tongdaway.util.blocks.ModBlocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.mojang.text2speech.Narrator.LOGGER;

public class TongDaWay implements ModInitializer {
    public static final String MODID = Common.MODID;
    Path configDir;
    Path configFile;
    private Gson gson;

    @Override
    public void onInitialize() {
        ModBlocks.loadClass();
        ModBlockEntities.loadClass();

        gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .create();
        configDir = FabricLoader.getInstance().getConfigDir().resolve(MODID);
        if (!Files.exists(configDir)) {
            configDir.toFile().mkdirs();
        }
        configFile = configDir.resolve("config.json");

//        loadConfig();
    }

//    public void loadConfig() {
//        LOGGER.info("Loading config file: {}", configFile);
//        try {
//            if (!Files.exists(configFile)) {
//                cfg = new WorldPreviewConfig();
//            } else {
//                cfg = gson.fromJson(Files.readString(configFile), WorldPreviewConfig.class);
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
