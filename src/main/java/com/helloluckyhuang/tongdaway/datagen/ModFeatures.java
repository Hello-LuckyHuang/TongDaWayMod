package com.helloluckyhuang.tongdaway.datagen;

import com.helloluckyhuang.tongdaway.Tongdaway;
import com.helloluckyhuang.tongdaway.worldgen.WayFeatureConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import static com.helloluckyhuang.tongdaway.event.FeatureRegistry.WAY_FEATURE;


public class ModFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> WAY_CONFIGURED_FEATURE_KEY = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Tongdaway.MODID, "way_and_cross"));
    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> pContext) {
        pContext.register(WAY_CONFIGURED_FEATURE_KEY,
                new ConfiguredFeature<>(WAY_FEATURE, new WayFeatureConfig(0)));
    }
}
