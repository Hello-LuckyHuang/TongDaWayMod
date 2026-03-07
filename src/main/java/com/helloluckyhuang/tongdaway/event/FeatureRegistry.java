package com.helloluckyhuang.tongdaway.event;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.worldgen.WayFeature;
import com.helloluckyhuang.tongdaway.worldgen.WayFeatureConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.RegisterEvent;

public class FeatureRegistry {
    public static final WayFeature WAY_FEATURE = new WayFeature(WayFeatureConfig.CODEC);
    public static final ResourceKey<Feature<?>> WAY_FEATURE_KEY = ResourceKey.create(Registries.FEATURE,
            ResourceLocation.fromNamespaceAndPath(TongDaWay.MODID, "way_and_cross"));

    private FeatureRegistry() {
    }

    public static void register(RegisterEvent event) {
        event.register(Registries.FEATURE,
                helper -> helper.register(WAY_FEATURE_KEY, WAY_FEATURE));
    }
}
