package com.helloluckyhuang.tongdaway.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TrackSpawnerBlockEntity extends BlockEntity {
    public TrackSpawnerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TRACK_SPAWNER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TrackSpawnerBlockEntity entity) {

    }
}
