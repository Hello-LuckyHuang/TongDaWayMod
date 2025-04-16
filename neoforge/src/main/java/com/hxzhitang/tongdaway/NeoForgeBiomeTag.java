package com.hxzhitang.tongdaway;

import com.google.auto.service.AutoService;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;

@AutoService(BiomeTag.class)
public final class NeoForgeBiomeTag implements BiomeTag {
    @Override
    public boolean isDry(Holder<Biome> biome) {
        return biome.is(Tags.Biomes.IS_DRY);
    }

    @Override
    public boolean isCold(Holder<Biome> biome) {
        return biome.is(Tags.Biomes.IS_COLD);
    }

    @Override
    public boolean isWet(Holder<Biome> biome) {
        return biome.is(Tags.Biomes.IS_WET);
    }
}
