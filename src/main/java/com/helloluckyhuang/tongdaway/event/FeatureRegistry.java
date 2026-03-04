package com.helloluckyhuang.tongdaway.event;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.worldgen.RailwayFeature;
import com.helloluckyhuang.tongdaway.worldgen.RailwayFeatureConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.RegisterEvent;

public class FeatureRegistry {
    public static final RailwayFeature RAILWAY_FEATURE = new RailwayFeature(RailwayFeatureConfig.CODEC);
    public static final ResourceKey<Feature<?>> RAILWAY_FEATURE_KEY = ResourceKey.create(Registries.FEATURE,
            ResourceLocation.fromNamespaceAndPath(TongDaWay.MODID, "way_and_cross"));

    private FeatureRegistry() {
    }

    public static void register(RegisterEvent event) {
        event.register(Registries.FEATURE,
                helper -> helper.register(RAILWAY_FEATURE_KEY, RAILWAY_FEATURE));
    }
}
