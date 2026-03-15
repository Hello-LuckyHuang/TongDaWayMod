package com.helloluckyhuang.tongdaway.way;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.util.AdaptiveHeightSampler;
import com.helloluckyhuang.tongdaway.util.SaveWayData;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.*;
import java.util.concurrent.*;

import static com.helloluckyhuang.tongdaway.TongDaWay.CHUNK_GROUP_SIZE;

public class WayBuilder {
    private static WayBuilder instance;
    private static long seed;

    private final Map<RegionPos, Future<?>> regionFutures = new ConcurrentHashMap<>();
    public final Map<RegionPos, WayMap> regionWays = new ConcurrentHashMap<>();
    public final Map<RegionPos, int[][]> regionHeightMap = new ConcurrentHashMap<>();
    public final Map<RegionPos, int[][]> regionStructureMap = new ConcurrentHashMap<>();
    public final Map<RegionPos, List<Pair<String, BlockPos>>> regionStructures = new ConcurrentHashMap<>();

    private final LinkedBlockingQueue<Runnable> regionWayLoadQueue = new LinkedBlockingQueue<Runnable>(); //线程池
    private final ThreadPoolExecutor regionWayLoadPoolExecutor = new ThreadPoolExecutor(64, 1024, 1, TimeUnit.DAYS, regionWayLoadQueue);
    private final WorldGenRegion level;

    public static final int samplingNum = 2; // 每个区块的采样数

    private WayBuilder(WorldGenRegion level) {
        this.level = level;
    }
    public static synchronized WayBuilder getInstance(long seed, WorldGenRegion level) {
        if (instance == null || WayBuilder.seed != seed) {
            instance = new WayBuilder(level);
            WayBuilder.seed = seed;
        }
        return instance;
    }

    public static synchronized WayBuilder getInstance(long seed) {
        if (instance == null || WayBuilder.seed != seed) {
            return null;
        }
        return instance;
    }

    // 为区块生成铁路路线。如未生成则阻塞线程开始生成。如已生成直接返回。
    // 这里只生成规划路线，不实际放置路线！
    public void generateWay(RegionPos regionPos) {
        // 如果路线已经生成，直接返回
        if (regionWays.containsKey(regionPos)) {
            return;
        }

        // 尝试从本地数据中读取
        WayMap savedData = SaveWayData.getWayMap(regionPos, level.getServer());
        if (savedData != null) {
            regionWays.put(regionPos, savedData);
            TongDaWay.LOGGER.info("Region {} Done! Read From Local Data", regionPos);
            return;
        }

        // 如果路线还未生成...
        try {
            // 如果没有线程在生成路线，添加线程开始生成
            if (!regionFutures.containsKey(regionPos)) {
                var f = regionWayLoadPoolExecutor.submit(() -> {
                    // 生成铁路步骤...
                    WayMap wayMap = new WayMap(regionPos);

                    wayMap.startPlanningRoutes(level);

                    // 放置路线规划结果
                    regionWays.put(regionPos, wayMap);

                    //将数据保存到磁盘
                    SaveWayData.putWayMap(regionPos, wayMap, level.getServer());
                });
                regionFutures.put(regionPos, f);
            }
            // 等待线程生成
            regionFutures.get(regionPos).get();
        } catch (InterruptedException | ExecutionException e) {
            TongDaWay.LOGGER.error("Gen way Err: ", e);
        } finally {
            regionFutures.remove(regionPos);
        }
    }

    /*
    * 缓存图管理
    * 旨在删除主播之前写的错乱的各种坐标系统
    * 统一使用世界坐标
    *
    * */

    /**
     * 获取指定坐标的高度
     * @param wx 世界坐标x
     * @param wz 世界坐标z
     * @return 高度
     */
    public int getHeight(int wx, int wz) {
        RegionPos regionPos = new RegionPos(Math.floorDiv(wx, 16*CHUNK_GROUP_SIZE), Math.floorDiv(wz, 16*CHUNK_GROUP_SIZE));
        int[][] heightMap = regionHeightMap
                .computeIfAbsent(regionPos, k -> getHeightMap(level.getLevel(), regionPos));
        int px = Math.floorDiv(wx - regionPos.x()*CHUNK_GROUP_SIZE*16, 16/samplingNum);
        int pz = Math.floorDiv(wz - regionPos.z()*CHUNK_GROUP_SIZE*16, 16/samplingNum);
        return heightMap[px][pz];
    }

    /**
     * 获取指定坐标的结构途径成本
     * @param wx 世界坐标x
     * @param wz 世界坐标z
     * @return 结构途径成本
     */
    public int getStructureCost(int wx, int wz) {
        RegionPos regionPos = new RegionPos(Math.floorDiv(wx, 16*CHUNK_GROUP_SIZE), Math.floorDiv(wz, 16*CHUNK_GROUP_SIZE));
        if (!regionStructures.containsKey(regionPos)) {
            Pair<int[][], List<Pair<String, BlockPos>>> pair = getStructureMap(level, regionPos);
            regionStructureMap.put(regionPos, pair.getFirst());
            regionStructures.put(regionPos, pair.getSecond());
        }
        int[][] costMap = regionStructureMap.get(regionPos);
        int px = Math.floorDiv(wx - regionPos.x()*CHUNK_GROUP_SIZE*16, 16/samplingNum);
        int pz = Math.floorDiv(wz - regionPos.z()*CHUNK_GROUP_SIZE*16, 16/samplingNum);
        return costMap[px][pz];
    }

    private int[][] getHeightMap(ServerLevel serverLevel, RegionPos regionPos) {
        // 高度自适应采样地形高度图
        ChunkGenerator gen = serverLevel.getChunkSource().getGenerator();
        RandomState cfg = serverLevel.getChunkSource().randomState();

        // 创建采样器：阈值=10，最大层数=3，每个节点4x4采样
        AdaptiveHeightSampler sampler = new AdaptiveHeightSampler(10, 3, 4, (x, z) -> {
            int wx = (int) (x*(16.0/samplingNum) + regionPos.x()*CHUNK_GROUP_SIZE*16);
            int wz = (int) (z*(16.0/samplingNum) + regionPos.z()*CHUNK_GROUP_SIZE*16);
            return gen.getBaseHeight(wx, wz, Heightmap.Types.WORLD_SURFACE_WG, serverLevel, cfg);
        });

        try {
            long startTime = System.currentTimeMillis();
            // 构建四叉树，区域大小
            sampler.buildQuadTree(CHUNK_GROUP_SIZE*samplingNum);
            long endTime = System.currentTimeMillis();
//            sampler.printStatistics();
            TongDaWay.LOGGER.info(" Region {} Build HeightMap time: {}ms", regionPos, endTime - startTime);
        } catch (InterruptedException e) {
            TongDaWay.LOGGER.error("Build HeightMap Err", e);
        } finally {
            sampler.shutdown();
        }

        int[][] heightMap = sampler.generateImage(CHUNK_GROUP_SIZE*samplingNum, CHUNK_GROUP_SIZE*samplingNum);

        return heightMap;
    }

    private Pair<int[][], List<Pair<String, BlockPos>>> getStructureMap(WorldGenRegion level, RegionPos regionPos) {
        // 计算遗迹
        var serverLevel = level.getLevel();
        var registryAccess = level.registryAccess();
        var chunkGeneratorStructureState = serverLevel.getChunkSource().getGeneratorState();
        var structureManager = serverLevel.structureManager();
        var structureFeatureManager = serverLevel.getStructureManager();

        var dimensionType = level.dimensionType();
        LevelHeightAccessor levelHeightAccessor = LevelHeightAccessor.create(dimensionType.minY(), dimensionType.height());

        List<Pair<String, BlockPos>> structurePos = new ArrayList<>();

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
                            var protoChunk = new ProtoChunk(new ChunkPos(regionPos.x() * CHUNK_GROUP_SIZE + finalGx, regionPos.z() * CHUNK_GROUP_SIZE + finalGz), UpgradeData.EMPTY, levelHeightAccessor, serverLevel.palettedContainerFactory(), null);
                            //计算和连接遗迹
                            serverLevel.getChunkSource().getGenerator().createStructures(registryAccess, chunkGeneratorStructureState, structureManager, protoChunk, structureFeatureManager, serverLevel.dimension());
                            var res = protoChunk.getAllStarts();
                            var structureRegistry = registryAccess.lookupOrThrow(Registries.STRUCTURE);
                            res.forEach((key, value) -> {
                                String structureName = Objects.requireNonNull(structureRegistry.getKey(key)).toString();
                                BlockPos pos = new BlockPos(protoChunk.getPos().x * 16, 0, protoChunk.getPos().z * 16);
                                structurePos.add(new Pair<>(structureName, pos));
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

        int[][] map = new int[CHUNK_GROUP_SIZE*samplingNum][CHUNK_GROUP_SIZE*samplingNum];
        for (Pair<String, BlockPos> pair : structurePos) {
            BlockPos pos = pair.getSecond();
            int[] p = new int[] {
                    (pos.getX() - regionPos.x()*CHUNK_GROUP_SIZE*16)*samplingNum/16,
                    (pos.getZ() - regionPos.z()*CHUNK_GROUP_SIZE*16)*samplingNum/16
            };
            for (int x = -5*samplingNum; x < 5*samplingNum; x++) {
                for (int z = -5*samplingNum; z < 5*samplingNum; z++) {
                    int px = p[0]+x;
                    int pz = p[1]+z;
                    if (px > 0 && px < map.length && pz > 0 && pz < map[0].length)
                        map[px][pz] = 600 - (Math.abs(x) + Math.abs(z));
                }
            }
        }

        return new Pair<>(map, structurePos);
    }
}
