package com.helloluckyhuang.tongdaway.way;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.way.planner.RoutePlanner;
import com.helloluckyhuang.tongdaway.way.planner.CrossPlanner;
import com.helloluckyhuang.tongdaway.util.AStarPathfinder;
import com.helloluckyhuang.tongdaway.util.CurveRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.helloluckyhuang.tongdaway.TongDaWay.CHUNK_GROUP_SIZE;

public class RailwayMap {
    public static final int samplingNum = 2; // 每个区块的采样数

    public final RegionPos regionPos;

    //********每个区域的数据*********
    // 路线
    public final Map<ChunkPos, Set<CurveRoute>> routeMap = new ConcurrentHashMap<>();
    // 车站
    public final List<CrossPlanner.CrossGenInfo> stations = new ArrayList<>();
    //********每个区域的数据*********

    public RailwayMap(RegionPos regionPos) {
        this.regionPos = regionPos;
    }

    // 规划路线方法
    public void startPlanningRoutes(WorldGenRegion level) {
        // 计算遗迹
        var serverLevel = level.getLevel();
        var registryAccess = level.registryAccess();
        var chunkGeneratorStructureState = serverLevel.getChunkSource().getGeneratorState();
        var structureManager = serverLevel.structureManager();
        var structureFeatureManager = serverLevel.getStructureManager();

        List<String> structures = new ArrayList<>();

        try(ExecutorService executor = Executors.newFixedThreadPool(16)) {
            // 创建线程池
            CountDownLatch latch = new CountDownLatch(CHUNK_GROUP_SIZE * CHUNK_GROUP_SIZE);
            for (int gx = 0; gx < CHUNK_GROUP_SIZE; gx++) {
                for (int gz = 0; gz < CHUNK_GROUP_SIZE; gz++) {
                    int finalGx = gx;
                    int finalGz = gz;
                    executor.execute(() -> {
                        try {
                            // 执行任务
                            ProtoChunk protoChunk = new ProtoChunk(new ChunkPos(regionPos.x() * CHUNK_GROUP_SIZE + finalGx, regionPos.z() * CHUNK_GROUP_SIZE + finalGz), UpgradeData.EMPTY, serverLevel, serverLevel.palettedContainerFactory(), null);
                            //计算和连接遗迹
                            serverLevel.getChunkSource().getGenerator().createStructures(registryAccess, chunkGeneratorStructureState, structureManager, protoChunk, structureFeatureManager, serverLevel.dimension());
                            var res = protoChunk.getAllStarts();
                            var structureRegistry = registryAccess.lookupOrThrow(Registries.STRUCTURE);
                            res.forEach((key, value) -> {
                                String structureName = Objects.requireNonNull(structureRegistry.getKey(key)).toString();
                                BlockPos pos = new BlockPos(protoChunk.getPos().x * 16, 0, protoChunk.getPos().z * 16);
//                                System.out.println(structureName + " " + pos);
                                structures.add(structureName);
                            });
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }

            // 等待所有任务完成
            latch.await();
            // 关闭线程池
            executor.shutdown();
        } catch (InterruptedException e) {
            TongDaWay.LOGGER.error("Search Feature Err: ", e);
        }

        // 生成损耗图
        RoutePlanner routePlanner = new RoutePlanner(regionPos);
        int[][] costMap = routePlanner.getCostMap(level);
        // 寻路专用
        int[][] costMapFindPath = routePlanner.getStructureCostMap(level);

        // 生成路口位置和连接规划
        CrossPlanner stationPlanner = new CrossPlanner(regionPos);
        stations.addAll(CrossPlanner.generateCross(regionPos, level.getLevel(), level.getSeed()));
        var connections = stationPlanner.generateConnections(level.getLevel(), level.getSeed());
        // 生成路线图
//        List<List<int[]>> test = new ArrayList<>();
        for (CrossPlanner.ConnectionGenInfo connection : connections) {
            // 转为损耗图下坐标系
            int[] picStart = AStarPathfinder.world2PicPos(connection.connectStart(), regionPos);
            int[] picEnd = AStarPathfinder.world2PicPos(connection.connectEnd(), regionPos);
            List<int[]> way = AStarPathfinder.findPath(costMap, picStart, picEnd,
                    (x, y) -> {
                        int scopeLimit = scopeLimit(x, y, picStart, picEnd);
                        int heightLimit = costMap[x][y] < level.getSeaLevel()+2 ? 100 : 0;
                        int structLimit = costMapFindPath[x][y];
                        return scopeLimit + heightLimit + structLimit;
                    });
//            test.add(way);
            // 设置出口坐标
            var route = routePlanner.getWay(way, costMap, connection, level.getLevel());
            putChunk(route);
        }
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

    public static int scopeLimit(int x, int z, int[] picStart, int[] picEnd) {
        // 限制寻路区域
        int maxCost = 10000; // 区域外消耗
        int A = 96;  // 限制区域最大宽度

        double length = new Vec2(picEnd[0]-picStart[0], picEnd[1]-picStart[1]).length();

        Vec3 p = new Vec3(x, 0, z);

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

        // 保存车站
        ListTag stationTag = new ListTag();
        stations.forEach(station -> stationTag.add(station.toNBT()));
        nbt.put("Stations", stationTag);

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

    public static RailwayMap fromNBT(CompoundTag nbt) {
        RegionPos regionPos = RegionPos.fromNBT((ListTag) nbt.get("RegionPos"));
        RailwayMap railwayMap = new RailwayMap(regionPos);

        // 读取车站
        ListTag stationTag = (ListTag) nbt.get("Stations");
        if (stationTag != null) {
            for (net.minecraft.nbt.Tag tag : stationTag) {
                CrossPlanner.CrossGenInfo station = CrossPlanner.CrossGenInfo.fromNBT((CompoundTag) tag);
                railwayMap.stations.add(station);
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
                    railwayMap.routeMap.put(chunkPos, routes);
                }
            }
        }

        return railwayMap;
    }
}