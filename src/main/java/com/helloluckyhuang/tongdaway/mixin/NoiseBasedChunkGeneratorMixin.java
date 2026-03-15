package com.helloluckyhuang.tongdaway.mixin;

import com.helloluckyhuang.tongdaway.structure.CrossTemplate;
import com.helloluckyhuang.tongdaway.way.WayBuilder;
import com.helloluckyhuang.tongdaway.way.RegionPos;
import com.helloluckyhuang.tongdaway.way.WayMap;
import com.helloluckyhuang.tongdaway.way.planner.CrossPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Inject(method = "buildSurface", at = @At("HEAD"))
    public void surfaceStart(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk, CallbackInfo ci) {
        var dimensionType = level.dimensionType();
        // 只有主世界生成路
        if (dimensionType.effectsLocation().toString().equals("minecraft:overworld")) {
            RegionPos regionPos = RegionPos.regionPosFromChunkPos(chunk.getPos());

            WayBuilder wayBuilder = WayBuilder.getInstance(level.getSeed(), level);
            wayBuilder.generateWay(regionPos);

            // 生成路口托盘
            WayMap wayMap = wayBuilder.regionWays.get(regionPos);
            if (wayMap != null) {
                for (CrossPlanner.CrossGenInfo crossPlace : wayMap.cross) {
                    var cross = crossPlace.crossTemplate();
                    if (cross == null) continue;
                    var pos = crossPlace.placePos();
                    var center = pos.getCenter();

                    if (cross.getTrayBoundChunks(center).contains(chunk.getPos())) {
                        tongDaWay2110$placeTray(chunk.getPos(), center, cross, chunk);
                    }
                }
            }
        }
    }

    @Unique
    private static void tongDaWay2110$placeTray(ChunkPos cPos, Vec3 center, CrossTemplate cross, ChunkAccess chunk) {
        Vec3 placeCenter = center.add(0, -3, 0);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int oy = -5; oy < 6; oy++) {
                    int y = oy + (int) placeCenter.y;
                    var p = new Vec3(cPos.x*16+x, y, cPos.z*16+z);
                    double d = cross.dis2Tray(placeCenter, p);
                    if (d < 3)
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 3);
                }
            }
        }
    }
}
