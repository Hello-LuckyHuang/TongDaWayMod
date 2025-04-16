package com.hxzhitang.tongdaway;

import com.hxzhitang.tongdaway.util.blocks.ModBlockEntities;
import com.hxzhitang.tongdaway.util.blocks.ModBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

// 20250211-0221
// The value here should match an entry in the META-INF/mods.toml file
@Mod(Tongdaway.MODID)
public class Tongdaway {
    // Define mod id in a common place for everything to reference
    public static final String MODID = Common.MODID;
    // Directly reference a slf4j logger

    public Tongdaway(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading

        // Register ourselves for server and other game events we are interested in
        ModBlocks.loadClass();
        ModBlockEntities.loadClass();

        NeoForgePlatformHandler.register(modEventBus);
        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
