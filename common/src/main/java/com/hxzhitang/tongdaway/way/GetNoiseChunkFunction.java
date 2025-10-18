package com.hxzhitang.tongdaway.way;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;

@FunctionalInterface
public interface GetNoiseChunkFunction {
    NoiseChunk getNoiseChunk(ChunkPos chunkPos, RandomState randomState);
}
