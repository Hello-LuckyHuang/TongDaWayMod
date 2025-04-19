package com.hxzhitang.tongdaway;

import com.google.auto.service.AutoService;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

@AutoService(BiomeTag.class)
public final class FabricBiomeTag implements BiomeTag {
    @Override
    public boolean isDry(Holder<Biome> biome) {
        return biome.is(ConventionalBiomeTags.IS_DRY);
    }

    @Override
    public boolean isCold(Holder<Biome> biome) {
        return biome.is(ConventionalBiomeTags.IS_COLD);
    }

    @Override
    public boolean isOcean(Holder<Biome> biome) {
        return biome.is(ConventionalBiomeTags.IS_OCEAN);
    }

    @Override
    public boolean isRiver(Holder<Biome> biome) {
        return biome.is(ConventionalBiomeTags.IS_RIVER);
    }
}
