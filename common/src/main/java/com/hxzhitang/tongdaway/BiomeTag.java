package com.hxzhitang.tongdaway;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.ServiceLoader;

public interface BiomeTag {
    BiomeTag TAG = load(BiomeTag.class);

    private static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Common.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }

    boolean isDry(Holder<Biome> biome);
    boolean isCold(Holder<Biome> biome);
    boolean isOcean(Holder<Biome> biome);
    boolean isRiver(Holder<Biome> biome);
}
