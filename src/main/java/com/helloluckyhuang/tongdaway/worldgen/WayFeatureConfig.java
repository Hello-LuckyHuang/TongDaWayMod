package com.helloluckyhuang.tongdaway.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record WayFeatureConfig(int placeHolder) implements FeatureConfiguration {
    public static final Codec<WayFeatureConfig> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Codec.INT.fieldOf("placeHolder").forGetter(WayFeatureConfig::placeHolder)
            ).apply(i, WayFeatureConfig::new)
    );
}
