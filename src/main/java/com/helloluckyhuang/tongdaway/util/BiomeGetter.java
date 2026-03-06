package com.helloluckyhuang.tongdaway.util;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class BiomeGetter {
    public static Holder<Biome> getBiome(ServerLevel level, Vec3 pos) {
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        var randomState = RandomState.create(
                ((NoiseBasedChunkGenerator) gen).generatorSettings().value(),
                level.registryAccess().lookupOrThrow(Registries.NOISE),
                level.getSeed()
        );
        return gen.getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock((int) pos.x),
                QuartPos.fromBlock((int) pos.y),
                QuartPos.fromBlock((int) pos.z),
                randomState.sampler()
        );
    }

    public static @NotNull String getBiomeId(ServerLevel level, Vec3 pos) {
        return getBiome(level, pos).getRegisteredName();
    }

    public static Holder<Biome> getBiomeFromId(String biomeIdString, ServerLevel level) {
        var registry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        ResourceLocation rl = ResourceLocation.parse(biomeIdString);
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, rl);

        return registry.get(key)
                .orElse(registry.getOrThrow(
                        ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "plains"))
                ));
    }

    public static String[] getBiomeTags(Holder<Biome> biome) {
        return biome.tags().map(TagKey::location).toList().stream().map(ResourceLocation::toString).toArray(String[]::new);
    }
}
