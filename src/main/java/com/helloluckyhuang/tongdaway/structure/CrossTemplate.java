package com.helloluckyhuang.tongdaway.structure;

import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class CrossTemplate extends ModTemplate {
    private final int id;

    public CrossTemplate(CompoundTag rootTag, int id, int heightOffset) {
        super(rootTag, heightOffset);
        this.id = id;
    }

    @Override
    public boolean isInVoxel(double x, double y, double z) {
        int originalX = (int) (x + Math.floor(getWidth() / 2.0));
        int originalY = (int) Math.floor(y) + heightOffset;
        int originalZ = (int) (z + Math.floor(getDepth() / 2.0));

        return originalX >= 0 && originalX < getWidth() && originalY >= 0 && originalY < getHeight() && originalZ >= 0 && originalZ < getDepth();
    }

    @Override
    public BlockState getBlockState(double x, double y, double z) {
        int originalX = (int) (x + Math.floor(getWidth() / 2.0));
        int originalY = (int) Math.floor(y) + heightOffset;
        int originalZ = (int) (z + Math.floor(getDepth() / 2.0));

        var blockState = voxelGrid.getBlockState(originalX, originalY, originalZ);

        if (blockState != null && blockState.is(Blocks.JIGSAW))
            return Blocks.AIR.defaultBlockState();

        return blockState;
    }

    public Set<ChunkPos> getBoundChunks(Vec3 center) {
        Set<ChunkPos> chunks = new HashSet<>();

        // 计算区域的AABB（轴对齐边界框）
        double minX = center.x - getWidth() / 2.0;
        double maxX = center.x + getWidth() / 2.0;
        double minZ = center.z - getDepth() / 2.0;
        double maxZ = center.z + getDepth() / 2.0;

        // 转换为区块坐标（一个区块是16x16x256格）
        int minChunkX = (int) Math.floor(minX / 16.0);
        int maxChunkX = (int) Math.floor(maxX / 16.0);
        int minChunkZ = (int) Math.floor(minZ / 16.0);
        int maxChunkZ = (int) Math.floor(maxZ / 16.0);

        // 遍历所有涉及的区块
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(new ChunkPos(chunkX, chunkZ));
            }
        }

        return chunks;
    }

    public int getId() {
        return id;
    }
}
