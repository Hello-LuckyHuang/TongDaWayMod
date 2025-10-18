package com.hxzhitang.tongdaway;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hxzhitang.tongdaway.util.blocks.ModBlockEntities;
import com.hxzhitang.tongdaway.util.blocks.ModBlocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.mojang.text2speech.Narrator.LOGGER;

public class TongDaWay implements ModInitializer {
    public static final String MODID = Common.MODID;
    Path configDir;
    Path configFile;
    private static Gson gson = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    @Override
    public void onInitialize() {
        ModBlocks.loadClass();
        ModBlockEntities.loadClass();

        //加载配置文件
        configDir = FabricLoader.getInstance().getConfigDir().resolve(MODID);
        if (!Files.exists(configDir)) {
            configDir.toFile().mkdirs();
        }
        configFile = configDir.resolve("tongdaway_config.json");

        try {
            Config config = loadConfig(configFile.toString());
            ConfigVar.features = config.getFeatures();
            ConfigVar.notes = config.getNotes();
            //LOGGER.info(ConfigVar.notes.toString());
            ConfigVar.alwaysConnectVillage = config.isConnectVillage();
            ConfigVar.connectFeaturesNum = config.getConnectFeaturesNum();
        } catch (Exception e) {
            LOGGER.error("TongDaWay Mod: Can't load config file!");
        }
    }

    /**
     * 加载配置文件
     * @param filePath 配置文件路径
     * @return 配置对象
     */
    private static Config loadConfig(String filePath) {
        Path path = Paths.get(filePath);

        // 如果文件不存在，创建默认配置并保存
        if (!Files.exists(path)) {
            Config defaultConfig = new Config();
            saveConfig(filePath, defaultConfig);
            return defaultConfig;
        }

        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(filePath), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, Config.class);
        } catch (IOException e) {
            LOGGER.error("Error loading config file: {}", e.getMessage());
            return new Config(); // 返回默认配置
        }
    }

    /**
     * 保存配置文件
     * @param filePath 配置文件路径
     * @param config 配置对象
     */
    private static void saveConfig(String filePath, Config config) {
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            LOGGER.error("Error saving config file: {}", e.getMessage());
        }
    }
}
