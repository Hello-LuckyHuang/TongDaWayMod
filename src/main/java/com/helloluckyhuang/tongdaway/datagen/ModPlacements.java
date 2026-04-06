package com.helloluckyhuang.tongdaway.datagen;

import com.helloluckyhuang.tongdaway.Tongdaway;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import static com.helloluckyhuang.tongdaway.datagen.ModFeatures.WAY_CONFIGURED_FEATURE_KEY;


public class ModPlacements {
    public static final ResourceKey<PlacedFeature> WAY_PLACED_FEATURE_KEY = ResourceKey.create(
            Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Tongdaway.MODID, "way_and_cross"));

    public static void bootstrap(BootstapContext<PlacedFeature> pContext) {
        HolderGetter<ConfiguredFeature<?, ?>> lookup = pContext.lookup(Registries.CONFIGURED_FEATURE);
        pContext.register(WAY_PLACED_FEATURE_KEY,
                new PlacedFeature(lookup.getOrThrow(WAY_CONFIGURED_FEATURE_KEY), java.util.List.of(InSquarePlacement.spread())));
    }
}
