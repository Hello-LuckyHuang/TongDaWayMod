package com.helloluckyhuang.tongdaway.way;

import com.helloluckyhuang.tongdaway.Config;
import com.helloluckyhuang.tongdaway.util.MyRandom;
import com.helloluckyhuang.tongdaway.way.planner.RoutePlanner;
import com.helloluckyhuang.tongdaway.way.planner.CrossPlanner;
import com.helloluckyhuang.tongdaway.util.AStarPathfinder;
import com.helloluckyhuang.tongdaway.util.CurveRoute;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.helloluckyhuang.tongdaway.TongDaWay.CHUNK_GROUP_SIZE;

public class WayMap {
    public final RegionPos regionPos;

    //********每个区域的数据*********
    // 路线
    public final Map<ChunkPos, Set<CurveRoute>> routeMap = new ConcurrentHashMap<>();
    // 路口
    public final List<CrossPlanner.CrossGenInfo> cross = new ArrayList<>();
    // 路上地物
    public final List<RoadFeature> roadFeature = new ArrayList<>();
    //********每个区域的数据*********

    public WayMap(RegionPos regionPos) {
        this.regionPos = regionPos;
    }

    // 规划路线方法
    public void startPlanningRoutes(WorldGenRegion level) {
        var builder = WayBuilder.getInstance(level.getSeed());
        ChunkGenerator gen = level.getLevel().getChunkSource().getGenerator();
        RandomState cfg = level.getLevel().getChunkSource().randomState();

        // 路口路线生成
        CrossPlanner crossPlanner = new CrossPlanner(regionPos);
        RoutePlanner routePlanner = new RoutePlanner();

        cross.addAll(CrossPlanner.generateCross(regionPos, level.getLevel(), level.getSeed()));
        var connections = crossPlanner.generateConnections(level.getLevel(), level.getSeed());
        // 生成路线图
        Map<Integer, int[]> points = new HashMap<>();
//        List<Set<int[]>> test = new ArrayList<>();
        for (CrossPlanner.ConnectionGenInfo connection : connections) {
            int[] picStart = connection.connectStart();
            int[] picEnd = connection.connectEnd();
            List<int[]> way = AStarPathfinder.findPath(builder, picStart, Set.of(picEnd), regionPos, 1,
                    (x, y) -> {
                        int scopeLimit = scopeLimit(x, y, picStart, picEnd);
                        int heightLimit = builder.getHeight(x, y) < level.getSeaLevel()+4 ? 100 : 0;
                        int structLimit = builder.getStructureCost(x, y);
                        return scopeLimit + heightLimit + structLimit;
                    });
            // 生成路径
            var result = routePlanner.getWay(way, connection, level);
            result.getSecond().forEach(p -> points.put(p[0]*31+p[1], p));
//            test.add(new HashSet<>(result.getSecond()));
            var route = result.getFirst();
            putChunk(route);
            // 生成路径上的地物
            genRoadFeature(route, level.getLevel());
        }

        // 连接结构
        if (builder != null) {
            List<Pair<String, BlockPos>> structures = builder.regionStructures.get(regionPos);
            List<Pair<String, BlockPos>> filter = structures.stream()
                    .filter(p ->
                            (Config.alwaysConnectVillage&&p.getFirst().contains("village"))
                            || Config.features.contains(p.getFirst())
                    )
                    .toList();
            List<Pair<String, BlockPos>> select = MyRandom.pickRandom(filter, Config.connectFeaturesNum, regionPos.hashCode());
            for (Pair<String, BlockPos> pair : select) {
                String name = pair.getFirst();
                if (name.contains(":"))
                    name = name.split(":")[1];
                name = name.replace("_", " ");
                BlockPos bPos = pair.getSecond();
                int[] start = new int[] {bPos.getX(), bPos.getZ()};
                List<int[]> way = AStarPathfinder.findPath(builder, start, new HashSet<>(points.values()), regionPos, 0,
                        (x, y) -> {
                            int heightLimit = builder.getHeight(x, y) < level.getSeaLevel()+4 ? 100 : 0;
                            int structLimit = builder.getStructureCost(x, y);
                            return heightLimit + structLimit;
                        });
                if (way.size() < 50)
                    continue;

                way.subList(0, 10).clear();
                // 生成路径
                start = way.getFirst();
                int[] end = points.get(way.getLast()[0]*31+way.getLast()[1]);

                int h = gen.getBaseHeight(start[0], start[1], Heightmap.Types.WORLD_SURFACE, level, cfg);
                Vec3 startPos = new Vec3(start[0], h, start[1]);

                CrossPlanner.ConnectionGenInfo connection = new CrossPlanner.ConnectionGenInfo(
                        startPos,
                        new Vec3(end[0], end[2], end[1]),
                        new int[] {(int) startPos.x, (int) startPos.z, (int) startPos.y},
                        end,
                        name
                );
                var result = routePlanner.getWay(way, connection, level);
                result.getSecond().forEach(p -> points.put(p[0]*31+p[1], p));
                var route = result.getFirst();
//                test.add(new HashSet<>(result.getSecond()));
                putChunk(route);
                // 生成路径上的地物
                genRoadFeature(route, level.getLevel());
//                System.out.println(end[0] + " " + end[2] + " " + end[1]);
            }
        }
//        for (Set<int[]> ints : test) {
//            for (int[] anInt : ints) {
//                anInt[0] = (anInt[0] - (regionPos.x()-1) * CHUNK_GROUP_SIZE * 16)/8;
//                anInt[1] = (anInt[1] - (regionPos.z()-1) * CHUNK_GROUP_SIZE * 16)/8;
//            }
//        }

//        ArrayToPNG.saveArrayAsPNG(new int[2048*3/8][2048*3/8], test, "D:\\测试噪声图\\"+level.getSeed()+"_"+regionPos+".png");
    }

    /**
     * 添加路径到区块
     * @param route 路径
     */
    private void putChunk(RoutePlanner.ResultWay route) {
        for (CurveRoute.CurveSegment segment : route.way().getSegments()) {
            for (Vec3 p : segment.rasterize(16)) {
                for (int i = -1; i < 2; i++) {
                    for (int j = -1; j < 2; j++) {
                        int cx = (int) Math.floor(p.x) + i;
                        int cz = (int) Math.floor(p.z) + j;
                        if (cx >= regionPos.x()*CHUNK_GROUP_SIZE && cx < (regionPos.x()+1)*CHUNK_GROUP_SIZE && cz >= regionPos.z()*CHUNK_GROUP_SIZE && cz < (regionPos.z()+1)*CHUNK_GROUP_SIZE) {
                            routeMap.computeIfAbsent(new ChunkPos(cx, cz), k -> new HashSet<>())
                                    .add(route.way());
                        }
                    }
                }
            }
        }
    }

    /**
     * 生成路径上的地物
     * @param route 路径
     */
    private void genRoadFeature(RoutePlanner.ResultWay route, ServerLevel level) {
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState cfg = level.getChunkSource().randomState();

        int step = 24;
        var list = route.way().getSegments();
        int length = 0;
        int totalLength = (int) route.way().getTotalLength();
        for (int i = 1; i < list.size() - 1; i++) {
            var seg = list.get(i);
            if (seg.getType().equals("bridge")) continue;

            int num = (int) Math.floor(seg.getLength() / step);
            for (int j = 0; j < num; j++) {
                Vec3 p = seg.getPointAt(j * step / seg.getLength());
                BlockPos pos = new BlockPos((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.z));
                // 地下不生成地物
                int h = gen.getBaseHeight(pos.getX(), pos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level, cfg);
                if (pos.getY() < h - 8) continue;

                String showText = "§d§lWay\n"+length+"m \n=> "+totalLength+"m\n§6"+(seg.getNote().isEmpty()?"★":seg.getNote());

                String type = i % 8 == 1 ? "sign" : "lamp";
                String notes = i % 8 == 1 ? showText : "";
                roadFeature.add(new RoadFeature(pos, type, seg.getBiome(), notes));
            }

            length += (int) seg.getLength();
        }
    }

    private static int scopeLimit(int x, int z, int[] picStart, int[] picEnd) {
        // 限制寻路区域
        int maxCost = 10000; // 区域外消耗
        int A = 320;  // 限制区域最大宽度

        double length = new Vec2(picEnd[0]-picStart[0], picEnd[1]-picStart[1]).length();

        Vec3 p = new Vec3(x-picStart[0], 0, z-picStart[1]);

        Vec3 va = new Vec3(picEnd[0]-picStart[0], 0, picEnd[1]-picStart[1]).normalize();
        Vec3 vert = new Vec3(0, 1, 0);
        Vec3 vb = va.cross(vert);

        double a = p.dot(va) / length;
        if (a < 0 || a > 1)
            return maxCost;

        double b = Math.abs(p.dot(vb));
        double py = A * Math.sin(Math.PI * a);

        if (b > py)
            return maxCost;

        return 0;
    }

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("RegionPos", regionPos.toNBT());

        // 保存路上地物
        ListTag roadFeatureTag = new ListTag();
        roadFeature.forEach(feature -> roadFeatureTag.add(feature.toNBT()));
        nbt.put("RoadFeature", roadFeatureTag);

        // 保存路口
        ListTag crossTag = new ListTag();
        cross.forEach(cross -> crossTag.add(cross.toNBT()));
        nbt.put("Cross", crossTag);

        // 保存路线
        List<CurveRoute> palette = new ArrayList<>();
        ListTag routeMapTag = new ListTag();
        routeMap.forEach((pos, routes) -> {
            CompoundTag chunkNbt = new CompoundTag();
            chunkNbt.putInt("ChunkPosX", pos.x);
            chunkNbt.putInt("ChunkPosZ", pos.z);
            ListTag routesTag = new ListTag();
            for (CurveRoute route : routes) {
                int index;
                if (palette.contains(route)) {
                    index = palette.indexOf(route);
                } else {
                    palette.add(route);
                    index = palette.size() - 1;
                }
                routesTag.add(IntTag.valueOf(index));
            }
            chunkNbt.put("Routes", routesTag);
            routeMapTag.add(chunkNbt);
        });
        ListTag paletteTag = new ListTag();
        for (CurveRoute route : palette) {
            paletteTag.add(route.toNBT());
        }
        nbt.put("RouteMap", routeMapTag);
        nbt.put("RoutePalette", paletteTag);

        return nbt;
    }

    public static WayMap fromNBT(CompoundTag nbt) {
        RegionPos regionPos = RegionPos.fromNBT((ListTag) nbt.get("RegionPos"));
        WayMap wayMap = new WayMap(regionPos);

        // 读取路上地物
        ListTag roadFeatureTag = (ListTag) nbt.get("RoadFeature");
        if (roadFeatureTag != null) {
            for (net.minecraft.nbt.Tag tag : roadFeatureTag) {
                RoadFeature feature = RoadFeature.fromNBT((CompoundTag) tag);
                wayMap.roadFeature.add(feature);
            }
        }

        // 读取路口
        ListTag crossTag = (ListTag) nbt.get("Cross");
        if (crossTag != null) {
            for (net.minecraft.nbt.Tag tag : crossTag) {
                CrossPlanner.CrossGenInfo cross = CrossPlanner.CrossGenInfo.fromNBT((CompoundTag) tag);
                wayMap.cross.add(cross);
            }
        }

        // 读取路径
        ListTag routeTag = (ListTag) nbt.get("RouteMap");
        ListTag paletteTag = (ListTag) nbt.get("RoutePalette");
        List<CurveRoute> palette = new ArrayList<>();
        if (paletteTag != null && routeTag != null) {
            for (Tag tag : paletteTag) {
                palette.add(CurveRoute.fromNBT((ListTag) tag));
            }
            for (net.minecraft.nbt.Tag tag : routeTag) {
                CompoundTag chunkNbt = (CompoundTag) tag;
                ChunkPos chunkPos = new ChunkPos(chunkNbt.getIntOr("ChunkPosX", 0), chunkNbt.getIntOr("ChunkPosZ", 0));
                if (chunkNbt.contains("Routes")) {
                    Set<CurveRoute> routes = new HashSet<>();
                    for (net.minecraft.nbt.Tag tag1 : chunkNbt.getListOrEmpty("Routes")) {
                        int index = ((IntTag) tag1).intValue();
                        CurveRoute route = palette.get(index);
                        routes.add(route);
                    }
                    wayMap.routeMap.put(chunkPos, routes);
                }
            }
        }

        return wayMap;
    }

    public record RoadFeature(BlockPos pos, String type, String biomeId, String notes) {
        public CompoundTag toNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("PosX", pos.getX());
            nbt.putInt("PosY", pos.getY());
            nbt.putInt("PosZ", pos.getZ());
            nbt.putString("Type", type);
            nbt.putString("BiomeId", biomeId);
            nbt.putString("Notes", notes);
            return nbt;
        }

        public static RoadFeature fromNBT(CompoundTag nbt) {
            int x = nbt.getIntOr("PosX", 0);
            int y = nbt.getIntOr("PosY", 0);
            int z = nbt.getIntOr("PosZ", 0);
            String type = nbt.getStringOr("Type", "");
            String biomeId = nbt.getStringOr("BiomeId", "");
            String notes = nbt.getStringOr("Notes", "");
            return new RoadFeature(new BlockPos(x, y, z), type, biomeId, notes);
        }
    }
}
