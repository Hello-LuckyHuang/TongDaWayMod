package com.helloluckyhuang.tongdaway.worldgen;

import com.helloluckyhuang.tongdaway.structure.CrossTemplate;
import com.helloluckyhuang.tongdaway.structure.ModStructureManager;
import com.helloluckyhuang.tongdaway.structure.RoadFeatureTemplate;
import com.helloluckyhuang.tongdaway.util.BiomeGetter;
import com.helloluckyhuang.tongdaway.way.RailwayBuilder;
import com.helloluckyhuang.tongdaway.way.WayMap;
import com.helloluckyhuang.tongdaway.way.RegionPos;
import com.helloluckyhuang.tongdaway.way.planner.CrossPlanner;
import com.helloluckyhuang.tongdaway.structure.RoadTemplate;
import com.helloluckyhuang.tongdaway.util.CurveRoute;
import com.helloluckyhuang.tongdaway.util.MyMth;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

public class RailwayFeature extends Feature<RailwayFeatureConfig> {
    public RailwayFeature(Codec<RailwayFeatureConfig> codec) {
        super(codec);
    }

    // -5732913011330067995
    @Override
    public boolean place(@NotNull FeaturePlaceContext<RailwayFeatureConfig> ctx) {
        ChunkPos cPos = new ChunkPos(ctx.origin());
        RegionPos regionPos = MyMth.regionPosFromChunkPos(cPos);
        WorldGenLevel world = ctx.level();
        ChunkAccess chunk = world.getChunk(cPos.x, cPos.z);

        RailwayBuilder builder = RailwayBuilder.getInstance(ctx.level().getSeed());
        if (builder == null) return false;

        WayMap wayMap = builder.regionRailways.get(regionPos);
        if (wayMap == null) return false;

        // 根据路线生成路基
        if (builder.regionRailways.containsKey(regionPos)) {
            if (wayMap.routeMap.containsKey(cPos)) {
                placeRoad(wayMap, cPos, chunk, world);
            }
        }

        // 放置路上地物
        long seed = regionPos.hashCode();
        for (WayMap.RoadFeature feature : wayMap.roadFeature) {
            var pos = feature.pos();
            var center = pos.getCenter();
            var type = feature.type();

            var biome = BiomeGetter.getBiomeFromId(feature.biomeId(), world.getLevel());
            var tags = BiomeGetter.getBiomeTags(biome);

            if (type.equals("lamp")) {
                RoadFeatureTemplate lamp = ModStructureManager.roadFeature.get(seed, "lamp", tags);
                if (lamp.getBoundChunks(center).contains(cPos)) {
                    placeRoadFeature(lamp, cPos, center, chunk);
                }
            }
        }

        // 放置路口
        for (CrossPlanner.CrossGenInfo crossPlace : wayMap.cross) {
            var cross = crossPlace.crossTemplate();
            if (cross == null) continue;
            var pos = crossPlace.placePos();
            var center = pos.getCenter();

            if (cross.getBoundChunks(center).contains(cPos)) {
                placeCross(cross, cPos, center, chunk);
            }
        }

        return true;
    }

    private static void placeCross(CrossTemplate cross, ChunkPos cPos, Vec3 center, ChunkAccess chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                var test = new Vec3(cPos.x*16+x, center.y + 1, cPos.z*16+z);
                if (!cross.isInVoxel(test.subtract(center).add(-0.5, -0.5, -0.5)))
                    continue;
                for (int oy = cross.getLowerBound(); oy < cross.getUpperBound(); oy++) {
                    int y = oy + (int) center.y;
                    var p = new Vec3(cPos.x*16+x, y, cPos.z*16+z).add(-0.5, -0.5, -0.5);
                    var blockState = cross.getBlockState(p.subtract(center));
                    if (blockState == null) {
                        // 应对机械动力蓝图保存的nbt文件不包含空气
                        if (cross.isInVoxel(p.subtract(center))) {
                            blockState = Blocks.AIR.defaultBlockState();
                        } else {
                            continue;
                        }
                    }
                    chunk.setBlockState(new BlockPos(x, y, z), blockState, 3);
                }
            }
        }
    }

    private static void placeRoadFeature(RoadFeatureTemplate feature, ChunkPos cPos, Vec3 center, ChunkAccess chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                var test = new Vec3(cPos.x*16+x, center.y + 1, cPos.z*16+z);
                if (!feature.isInVoxel(test.subtract(center).add(-0.5, -0.5, -0.5)))
                    continue;
                for (int oy = feature.getLowerBound(); oy < feature.getUpperBound(); oy++) {
                    int y = oy + (int) center.y;
                    var p = new Vec3(cPos.x*16+x, y, cPos.z*16+z).add(-0.5, -0.5, -0.5);
                    var blockState = feature.getBlockState(p.subtract(center));
                    if (blockState == null || blockState.isAir()) {
                        continue;
                    }
                    chunk.setBlockState(new BlockPos(x, y, z), blockState, 3);
                }
            }
        }
    }

    private static void placeRoad(WayMap wayMap, ChunkPos cPos, ChunkAccess chunk, WorldGenLevel world) {
        var routes = wayMap.routeMap.get(cPos);
        for (CurveRoute route : routes) {
            int seed = route.getSegments().size();

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    // 获取一个线上点
                    var testPoint0 = new Vec3(cPos.x*16+x, 80, cPos.z*16+z);
                    CurveRoute.Frame frame = route.getFrame(testPoint0);

                    var seg = route.getSegments().get(frame.segmentIndex);
                    String type = seg.getType();
                    String biomeIdString = seg.getBiome();
                    Holder<Biome> biome = BiomeGetter.getBiomeFromId(biomeIdString, world.getLevel());
//                    RoadTemplate bridge = ModStructureManager.getRandomBridge(seed);

                    if (biome.is(Tags.Biomes.IS_OCEAN)) continue;

                    var nearest0 = frame.nearestPoint;

                    double t = frame.globalT;
                    var normal0 = frame.normal0;
                    var binormal0 = frame.binormal0;
                    var tangent0 = frame.tangent0;

                    Vec3 vec0 = testPoint0.subtract(nearest0);

                    double lx = vec0.dot(tangent0);
                    if (Math.abs(lx) > 4) continue;

                    // 根据曲线上高度和实际高度判断应用桥隧
                    BlockPos nearestPos = new BlockPos((int) nearest0.x, (int) nearest0.y, (int) nearest0.z);
                    int h = world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, nearestPos.getX(), nearestPos.getZ());

                    boolean conditionBridge = (nearest0.y > h + 5) || (chunk.getBlockState(new BlockPos((int) nearest0.x, world.getSeaLevel()-2, (int) nearest0.z)).is(Blocks.WATER));
                    boolean conditionTunnel = nearest0.y < h - 8;

                    // 随机获取一个路基，使用路线段数作为种子来选择
                    RoadTemplate structureTemplate;
                    if (type.equals("bridge")) {
                        structureTemplate = ModStructureManager.getRandomBridge(seed, BiomeGetter.getBiomeTags(biome));
                    } else {
                        if (conditionBridge) {
                            structureTemplate = ModStructureManager.getRandomShortBridge(seed, BiomeGetter.getBiomeTags(biome));
                        } else if (conditionTunnel) {
                            structureTemplate = ModStructureManager.getRandomTunnel(seed, BiomeGetter.getBiomeTags(biome));
                        } else {
                            structureTemplate = ModStructureManager.getRandomGround(seed, BiomeGetter.getBiomeTags(biome));
                        }
                    }

                    double localX = t * route.getTotalLength();

                    double z0 = vec0.dot(binormal0);

                    if (structureTemplate == null || !structureTemplate.isInVoxel(1, 1, z0))
                        continue;

                    for (int oy = structureTemplate.getUpperBound(); oy >= structureTemplate.getLowerBound(); oy--) {
                        int y = oy + (int) nearest0.y;
                        var testPoint = new Vec3(cPos.x*16+x, y, cPos.z*16+z);
                        var vec = testPoint.subtract(nearest0);

                        double localY = vec.dot(normal0) - 0.15;
                        double localZ = vec.dot(binormal0);

                        // 根据标架下坐标,从模板结构找到对应方块,并且放置
                        BlockState blockState = structureTemplate.getBlockState(localX, localY, localZ);
                        if (blockState != null) {
                            BlockPos blockPos = new BlockPos(cPos.getMinBlockX()+x, y, cPos.getMinBlockZ()+z);
                            world.setBlock(blockPos, blockState, 3);
                            BlockState updatedState = Block.updateFromNeighbourShapes(world.getBlockState(blockPos), world, blockPos);
                            world.setBlock(blockPos, updatedState, 3);

                            for (Direction direction : Direction.values()) {
                                BlockPos neighborPos = blockPos.relative(direction);
                                BlockState neighborState = Block.updateFromNeighbourShapes(world.getBlockState(neighborPos), world, neighborPos);
                                world.setBlock(neighborPos, neighborState, 3);
                            }
                        }
                    }
                    // 向下填充地基直到遇到支撑方块(隧道不考虑向下填充地基)
                    if (conditionTunnel)
                        continue;

                    for (int oy = structureTemplate.getLowerBound() - 1; oy > structureTemplate.getLowerBound() - 100; oy--) {
                        int y = oy + (int) nearest0.y;

                        BlockPos blockPos = new BlockPos(cPos.getMinBlockX()+x, y, cPos.getMinBlockZ()+z);

                        if (chunk.getBlockState(blockPos).isFaceSturdy(world, blockPos, Direction.UP)) {
                            break;
                        }

                        var testPoint = new Vec3(cPos.x*16+x, y, cPos.z*16+z);
                        var vec = testPoint.subtract(nearest0);

                        double localY = vec.dot(normal0) - 0.15;
                        double localZ = vec.dot(binormal0);

                        BlockState blockState = structureTemplate.getBlockState(localX, localY, localZ);
                        if (blockState != null) {
                            world.setBlock(blockPos, blockState, 3);
                            BlockState updatedState = Block.updateFromNeighbourShapes(world.getBlockState(blockPos), world, blockPos);
                            world.setBlock(blockPos, updatedState, 3);

                            for (Direction direction : Direction.values()) {
                                BlockPos neighborPos = blockPos.relative(direction);
                                BlockState neighborState = Block.updateFromNeighbourShapes(world.getBlockState(neighborPos), world, neighborPos);
                                world.setBlock(neighborPos, neighborState, 3);
                            }
                        }
                    }
                }
            }

        }
    }
}