package com.helloluckyhuang.tongdaway;

import com.helloluckyhuang.tongdaway.blocks.ModBlockEntities;
import com.helloluckyhuang.tongdaway.blocks.ModBlocks;
import com.helloluckyhuang.tongdaway.datagen.ModDataGen;
import com.helloluckyhuang.tongdaway.event.FeatureRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Tongdaway.MODID)
public class Tongdaway {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "tongdaway";
    // Mod常量
    public static final int CHUNK_GROUP_SIZE = 128;  // 一个路线生成区域的大小
    public static final int HEIGHT_MAX_INCREMENT = 120;  // 路线生成最大高度相对于海平面的增量
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();


    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Tongdaway() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        modEventBus.addListener(this::commonSetup);

        modEventBus.addListener(FeatureRegistry::register);

//        ModBlocks.register(modEventBus);
//        ModBlockEntities.register(modEventBus);

        modEventBus.addListener(ModDataGen::gatherData);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (TongDaWay) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }
}
