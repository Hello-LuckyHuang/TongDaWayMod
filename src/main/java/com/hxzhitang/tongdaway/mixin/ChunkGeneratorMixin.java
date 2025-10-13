package com.hxzhitang.tongdaway.mixin;

import com.hxzhitang.tongdaway.way.RegionPos;
import com.hxzhitang.tongdaway.way.RegionWayMap;
import com.hxzhitang.tongdaway.way.WayTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.hxzhitang.tongdaway.Tongdaway.CHUNK_GROUP_SIZE;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    @Shadow
    public abstract void createStructures(RegistryAccess p_255835_, ChunkGeneratorStructureState p_256505_, StructureManager p_255934_, ChunkAccess p_255767_, StructureTemplateManager p_255832_);

    @Unique
    protected final Map<RegionPos, RegionWayMap> tongDaWay$ChunkWays = new HashMap<>();

    @Inject(method = "applyBiomeDecoration", at = @At("HEAD"))
    public void applyBiomeDecorationStart(WorldGenLevel p_223087_, ChunkAccess p_223088_, StructureManager p_223089_, CallbackInfo ci) {
        var dimensionType = p_223087_.dimensionType();

        if (dimensionType.effectsLocation().toString().equals("minecraft:overworld")) {
            int x = p_223088_.getPos().x;
            int z = p_223088_.getPos().z;

            int px = (int) Math.floor(x / (double) CHUNK_GROUP_SIZE);
            int pz = (int) Math.floor(z / (double) CHUNK_GROUP_SIZE);

            RegionPos regionPos = new RegionPos(px, pz);
            if (tongDaWay$ChunkWays.containsKey(regionPos)) {
                List<RegionWayMap.WayPoint> chunkWayMap = tongDaWay$ChunkWays.get(regionPos).getWayMap(p_223088_.getPos());
                if (chunkWayMap != null) {
                    // 路基
                    for (RegionWayMap.WayPoint p : chunkWayMap) {
                        int ix = p.pos().getX();
                        int iz = p.pos().getZ();
                        int cy = p.pos().getY();

                        BlockPos realPos0 = new BlockPos(ix + 16 * x, cy, iz + 16 * z);
                        if (Objects.equals(p.pointType(), "way")) {
                            WayTools.wayFoundation(p_223087_, realPos0, p.pointCode());
                        } else if (Objects.equals(p.pointType(), "pier")) {
                            WayTools.buildPier(p_223087_, realPos0);
                        }
                    }
                    // 道路
                    for (RegionWayMap.WayPoint p : chunkWayMap) {
                        int ix = p.pos().getX();
                        int iz = p.pos().getZ();
                        int cy = p.pos().getY();
                        int h = p_223088_.getHeight(Heightmap.Types.WORLD_SURFACE_WG, ix, iz);

                        BlockPos realPos0 = new BlockPos(ix + 16 * x, cy, iz + 16 * z);
                        BlockPos realPos1 = new BlockPos(ix + 16 * x, h + 1, iz + 16 * z);
                        if (Objects.equals(p.pointType(), "way")) {
                            WayTools.buildWay(p_223087_, realPos0, p.pointCode());
                        } else if (Objects.equals(p.pointType(), "intersection")) {
                            WayTools.buildIntersection(p_223087_, p, p_223088_, realPos1);
                        } else if (Objects.equals(p.pointType(), "streetlight")) {
                            WayTools.buildStreetlight(p_223087_, p, p_223088_, realPos0);
                        } else if (Objects.equals(p.pointType(), "road_signs")) {
                            WayTools.buildWaySign(p_223087_, p, realPos0);
                        }
                    }
                }
            }
        }
    }
}
