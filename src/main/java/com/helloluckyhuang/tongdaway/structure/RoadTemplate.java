package com.helloluckyhuang.tongdaway.structure;

import net.minecraft.nbt.*;
import net.minecraft.world.level.block.state.BlockState;

public class RoadTemplate extends ModTemplate {
    private int roadbedHeight = 0;

    public RoadTemplate(CompoundTag nbt) {
        super(nbt);
    }

    @Override
    public boolean isInVoxel(double x, double y, double z) {
        int originalX = (int) Math.floor(x) % getWidth();
        int originalY = (int) Math.round(y + roadbedHeight + 1);
        int originalZ = (int) Math.floor(z + getDepth() / 2.0);

        return originalX >= 0 && originalX < getWidth() && originalY >= 0 && originalY < getHeight() && originalZ >= 0 && originalZ < getDepth();
    }

    // 坐标系原点在z方向中心方块的方块坐标处
    @Override
    public BlockState getBlockState(double x, double y, double z) {
        // 映射回原始体素坐标
        // 原始X坐标由曲线参数决定
        int originalX = (int) Math.floor(x) % getWidth();

        // 原始Y和Z坐标由局部坐标决定（考虑网格中心）
        int originalY = (int) Math.round(y + roadbedHeight + 1);  // 从路面高度计y坐标
        int originalZ = (int) Math.floor(z + getDepth() / 2.0);

        return voxelGrid.getBlockState(originalX, originalY, originalZ);
    }
}

