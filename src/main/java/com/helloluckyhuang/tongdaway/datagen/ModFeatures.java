package com.helloluckyhuang.tongdaway.datagen;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.worldgen.RailwayFeatureConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import static com.helloluckyhuang.tongdaway.event.FeatureRegistry.RAILWAY_FEATURE;


public class ModFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>>RAILWAY_CONFIGURED_FEATURE_KEY = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(TongDaWay.MODID, "way_and_cross"));
    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> pContext) {
        pContext.register(RAILWAY_CONFIGURED_FEATURE_KEY,
                new ConfiguredFeature<>(RAILWAY_FEATURE, new RailwayFeatureConfig(0)));
    }
}
