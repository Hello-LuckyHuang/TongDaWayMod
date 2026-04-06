package com.helloluckyhuang.tongdaway;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod.EventBusSubscriber(modid = Tongdaway.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TongDaWayClient {
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {

    }
}
