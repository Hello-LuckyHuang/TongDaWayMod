package com.hxzhitang.tongdaway.way;

import com.hxzhitang.tongdaway.ConfigVar;
import com.hxzhitang.tongdaway.mixin.NoiseChunkAccessor;
import com.hxzhitang.tongdaway.tools.ExpandImage;
import com.hxzhitang.tongdaway.tools.ImageGradient;
import com.hxzhitang.tongdaway.tools.OptimizedAStarEightDirections;
import com.hxzhitang.tongdaway.util.ModLevelSaveData;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

import static com.hxzhitang.tongdaway.Common.CHUNK_GROUP_BUFFER;
import static com.hxzhitang.tongdaway.Common.CHUNK_GROUP_SIZE;


public class ChunkGroup implements Runnable {
    private final int x;
    private final int z;

    private final WorldGenRegion worldGenRegion;
    private final CreateStructuresFunction structureFunc;
    private final GetNoiseChunkFunction getNoiseChunkFunc;
    private final RandomState random;
    private final NoiseGeneratorSettings noiseGeneratorSettings;
    private final Map<RegionPos, RegionWayMap> chunkWays;

    private final LinkedBlockingQueue<Runnable> linkedBlockingQueue = new LinkedBlockingQueue<Runnable>(); //线程池
    private final ThreadPoolExecutor heightMapLoadPoolExecutor;
    private boolean quickLoadHeightMap; //是否开启更多线程快速加载高度图

    public static final int blockStride = 8; // 采样步长,要16的约数

    private final long seed;

    public ChunkGroup(
            int x,
            int z,
            boolean quickLoadHeightMap,
            WorldGenRegion worldGenRegion,
            NoiseGeneratorSettings noiseGeneratorSettings,
            RandomState random,
            CreateStructuresFunction structureFunc,
            GetNoiseChunkFunction getNoiseChunkFunc,
            Map<RegionPos, RegionWayMap> tongDaWay$ChunkWays
    ) {
        this.x = x;
        this.z = z;
        this.worldGenRegion = worldGenRegion;
        this.structureFunc = structureFunc;
        this.getNoiseChunkFunc = getNoiseChunkFunc;
        this.seed = this.worldGenRegion.getSeed();
        this.random = random;
        this.noiseGeneratorSettings = noiseGeneratorSettings;
        this.chunkWays = tongDaWay$ChunkWays;
        //创建线程池
        this.quickLoadHeightMap = quickLoadHeightMap;
        if (quickLoadHeightMap) {
            heightMapLoadPoolExecutor = new ThreadPoolExecutor(64, 2048, 1, TimeUnit.DAYS, linkedBlockingQueue);
        } else {
            heightMapLoadPoolExecutor = new ThreadPoolExecutor(2, 2048, 1, TimeUnit.DAYS, linkedBlockingQueue);
        }
    }

    public boolean isQuickLoadHeightMap() {
        return quickLoadHeightMap;
    }

    public void setLoadHeightMapQuickly() {
        if (!this.quickLoadHeightMap) {
            this.quickLoadHeightMap = true;
            heightMapLoadPoolExecutor.setCorePoolSize(64);
        }
    }

    //搜索区块组内的村庄
    @Override
    public void run() {
        //从磁盘文件读取路径信息，若有，则放入集合直接返回。
        ModLevelSaveData data  = ModLevelSaveData.get(Objects.requireNonNull(worldGenRegion.getServer()).getLevel(ServerLevel.OVERWORLD));
        RegionWayMap savedRegionWay = data.getRegionWay(new RegionPos(this.x, this.z));
        if (savedRegionWay != null) {
            this.chunkWays.put(new RegionPos(this.x, this.z), savedRegionWay);
//            Tongdaway.LOGGER.info("Region {},{} Done! Read From Local Data", this.x, this.z);
            return;
        }

        long startTime = System.currentTimeMillis();
        //寻找村庄，生成高度图
        var dimensionType = worldGenRegion.dimensionType();
        int seaLevel = worldGenRegion.getSeaLevel();

        LevelHeightAccessor levelHeightAccessor = LevelHeightAccessor.create(dimensionType.minY(), dimensionType.height());

        var serverLevel = worldGenRegion.getLevel();
        var registryAccess = worldGenRegion.registryAccess();
        var chunkGeneratorStructureState = serverLevel.getChunkSource().getGeneratorState();
        var structureManager = serverLevel.structureManager();
        var structureFeatureManager = serverLevel.getStructureManager();

        var biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);

        int picLength = (CHUNK_GROUP_SIZE + CHUNK_GROUP_BUFFER * 2) * (16/blockStride);

        int[][] heightMap = new int[picLength][picLength];

        //生成路径图
        WayMap wayMap = new WayMap(this.x, this.z, this.seed);

        for (int gx = 0; gx < (CHUNK_GROUP_SIZE + CHUNK_GROUP_BUFFER * 2); gx++) {
            for (int gz = 0; gz < (CHUNK_GROUP_SIZE + CHUNK_GROUP_BUFFER * 2); gz++) {
                var protoChunk = new ProtoChunk(new ChunkPos(x * CHUNK_GROUP_SIZE + gx - CHUNK_GROUP_BUFFER, z * CHUNK_GROUP_SIZE + gz - CHUNK_GROUP_BUFFER), UpgradeData.EMPTY, levelHeightAccessor, biomeRegistry, null);
                if (protoChunk.getStatus() == ChunkStatus.EMPTY) {
                    int finalGx = gx;
                    int finalGz = gz;

                    heightMapLoadPoolExecutor.submit(() -> {
                        //计算高度图
                        var heightMapTile = getHeight(protoChunk.getPos(), blockStride, seaLevel);
                        for (int px = 0; px < 16/blockStride; px++) {
                            for (int pz = 0; pz < 16 / blockStride; pz++) {
                                int x = finalGx * 16 / blockStride + px;
                                int z = finalGz * 16 / blockStride + pz;
                                int y = heightMapTile[px][pz].y;
                                heightMap[x][z] = y;
                            }
                        }
                        //慢线程休息时间！
                        for (int t = 0; t < 10 && !quickLoadHeightMap; t++) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        //计算和连接遗迹
                        structureFunc.createStructures(registryAccess, chunkGeneratorStructureState, structureManager, protoChunk, structureFeatureManager);
                        var res = protoChunk.getAllStarts();
                        var structureRegistry = registryAccess.registryOrThrow(Registries.STRUCTURE);
                        res.forEach((key, value) -> {
                            String structureName = Objects.requireNonNull(structureRegistry.getKey(key)).toString();
                            BlockPos pos = new BlockPos(protoChunk.getPos().x * 16, heightMapTile[0][0].y, protoChunk.getPos().z * 16);
                            if (ConfigVar.features.contains(structureName)) {
                                //连接配置文件连接遗迹名单
                                wayMap.setStructureNode(pos, structureName);
                            } else if (ConfigVar.alwaysConnectVillage && structureName.contains("village")) {
                                //一定连接村庄
                                wayMap.setStructureNode(pos, structureName);
                            }
                        });
                    });
                }
            }
        }
        heightMapLoadPoolExecutor.shutdown();

        //等待所有线程执行完毕
        try {
            if (!heightMapLoadPoolExecutor.awaitTermination(120, TimeUnit.SECONDS)) {
                heightMapLoadPoolExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            heightMapLoadPoolExecutor.shutdownNow();
        }

        //计算高度图，生成合适路线
        int nodeNum = 0;
        RegionWayMap regionWayMap = new RegionWayMap(this.x, this.z);
        wayMap.applyStructureNode(); //生成遗迹路线
        for (Pair<BlockPos, BlockPos> p : wayMap.getWays().keySet()) {
            String wayName = wayMap.getWays().get(p);
            BlockPos pA = p.getFirst();//.getVertexPos();
            BlockPos pB = p.getSecond();//.getVertexPos();
            int pAx = pA.getX() >= 0 ? pA.getX() % (CHUNK_GROUP_SIZE * 16) : (CHUNK_GROUP_SIZE * 16 - 1) + ((pA.getX() + 1) % (CHUNK_GROUP_SIZE * 16));
            int pAz = pA.getZ() >= 0 ? pA.getZ() % (CHUNK_GROUP_SIZE * 16) : (CHUNK_GROUP_SIZE * 16 - 1) + ((pA.getZ() + 1) % (CHUNK_GROUP_SIZE * 16));
            int pBx = pB.getX() >= 0 ? pB.getX() % (CHUNK_GROUP_SIZE * 16) : (CHUNK_GROUP_SIZE * 16 - 1) + ((pB.getX() + 1) % (CHUNK_GROUP_SIZE * 16));
            int pBz = pB.getZ() >= 0 ? pB.getZ() % (CHUNK_GROUP_SIZE * 16) : (CHUNK_GROUP_SIZE * 16 - 1) + ((pB.getZ() + 1) % (CHUNK_GROUP_SIZE * 16));
            //根据高度图通过最短路径算法计算建议路径
            int[] start = {pAx, pAz};
            int[] end = {pBx, pBz};
            double[][] expandedHeightMap = ExpandImage.expandImage(heightMap, blockStride);
            double[][] slopeImg = ImageGradient.calculateGradient(expandedHeightMap);
            List<int[]> path = OptimizedAStarEightDirections.findMinimumCostPath(slopeImg, start, end, (x, y) -> heightMap[x/blockStride][y/blockStride] <= 63 ? 5000.0 : 0.0);
            //输入计算路径和原计算高度图（非插值后）,为路线生成提供依据
            regionWayMap.putWayMap(path, expandedHeightMap, seaLevel, wayName);

            nodeNum += path.size();
        }
        regionWayMap.putIntersection(wayMap); // 将路口加入到regionWayMap中
        chunkWays.put(new RegionPos(this.x, this.z), regionWayMap);
        //将数据保存到磁盘
        data.putRegionWay(new RegionPos(this.x, this.z), regionWayMap);

        long duration = System.currentTimeMillis() - startTime;
//        Common.LOGGER.info("Region {},{} Done! Way Nodes Num: {}, Quick Finish: {}, Use Time: {} ms", this.x, this.z, nodeNum, quickLoadHeightMap, duration);
    }

    /**
     * 预获取高度图，此部分计算地表高度代码参考自Taiterio的world preview
     * 见https://github.com/caeruleusDraconis/world-preview
     * @param chunkPos 区块坐标
     * @param blockStride 采样步长，要16的约数
     * @return 高度图
     */
    private HeightData[][] getHeight(ChunkPos chunkPos, int blockStride, int seaLevel) {
        final NoiseSettings noiseSettings = noiseGeneratorSettings.noiseSettings();
        final NoiseChunk noiseChunk = getNoiseChunkFunc.getNoiseChunk(chunkPos, random);
        final Predicate<BlockState> predicate = Heightmap.Types.OCEAN_FLOOR_WG.isOpaque();

        final int cellWidth = noiseSettings.getCellWidth();
        final int cellHeight = noiseSettings.getCellHeight();
        final int minY = noiseSettings.minY();
        final int maxY = minY + noiseSettings.height();
        final int cellMinY = Mth.floorDiv(minY, cellHeight);
        final int cellCountY = Mth.floorDiv(maxY - minY, cellHeight);
        final int cellOffsetY = cellMinY - Mth.floorDiv(noiseSettings.minY(), cellHeight);

        final int minBlockX = chunkPos.getMinBlockX();
        final int minBlockZ = chunkPos.getMinBlockZ();
        final int cellCountXZ = 16 / cellWidth;

        final int resultSize = 16 / blockStride;
        HeightData[][] result = new HeightData[resultSize][resultSize];

        try {
            noiseChunk.initializeForFirstCellX();
            for (int cellX = 0; cellX < cellCountXZ; cellX++) {
                noiseChunk.advanceCellX(cellX);

                for (int cellZ = 0; cellZ < cellCountXZ; cellZ++) {
                    List<XZPair> positions = new LinkedList<>();

                    // Precompute positions only for unpopulated result indices
                    for (int xInCell = 0; xInCell < cellWidth; xInCell += blockStride) {
                        for (int zInCell = 0; zInCell < cellWidth; zInCell += blockStride) {
                            int x = minBlockX + cellX * cellWidth + xInCell;
                            int z = minBlockZ + cellZ * cellWidth + zInCell;
                            int i = (x - minBlockX) / blockStride;
                            int j = (z - minBlockZ) / blockStride;

                            if (result[i][j] == null) {
                                positions.add(new XZPair(
                                        x, (double) xInCell / cellWidth,
                                        z, (double) zInCell / cellWidth,
                                        i, j
                                ));
                            }
                        }
                    }

                    // Early exit if all positions already filled
                    if (positions.isEmpty()) continue;

                    // Process Y layers from top to bottom
                    Iterator<XZPair> iterator = positions.iterator();
                    while (iterator.hasNext()) {
                        XZPair curr = iterator.next();
                        noiseChunk.updateForX(curr.x, curr.dX);
                        noiseChunk.updateForZ(curr.z, curr.dZ);

                        for (int cellY = cellCountY - 1; cellY >= 0; cellY--) {
                            noiseChunk.selectCellYZ(cellY + cellOffsetY, cellZ);

                            for (int yInCell = cellHeight - 1; yInCell >= 0; yInCell -= 2) {
                                final int y = (cellMinY + cellY) * cellHeight + yInCell;
                                noiseChunk.updateForY(y, (double) yInCell / cellHeight);

                                BlockState blockState = ((NoiseChunkAccessor) noiseChunk).invokeGetInterpolatedState();
                                if (blockState == null) {
                                    blockState = noiseGeneratorSettings.defaultBlock();
                                }

                                if (predicate.test(blockState)) {
                                    result[curr.i][curr.j] = new HeightData(curr.x, y + 1, curr.z);
                                    iterator.remove();
                                }

                                if (y < seaLevel - 4) {
                                    result[curr.i][curr.j] = new HeightData(curr.x, seaLevel - 4, curr.z);
                                    iterator.remove();
                                }

                                if (positions.isEmpty()) break;
                            }
                            if (positions.isEmpty()) break;
                        }
                    }


                }
                noiseChunk.swapSlices();
            }
        } finally {
            noiseChunk.stopInterpolation();
        }

        // Fill remaining null positions with default height
        for (int i = 0; i < resultSize; i++) {
            for (int j = 0; j < resultSize; j++) {
                if (result[i][j] == null) {
                    int x = minBlockX + i * blockStride;
                    int z = minBlockZ + j * blockStride;
                    result[i][j] = new HeightData(x, minY, z);
                }
            }
        }

        return result;
    }

    private static class XZPair {
        final int x;
        final double dX;
        final int z;
        final double dZ;
        final int i;
        final int j;

        XZPair(int x, double dX, int z, double dZ, int i, int j) {
            this.x = x;
            this.dX = dX;
            this.z = z;
            this.dZ = dZ;
            this.i = i;
            this.j = j;
        }
    }

    private record HeightData(int x, int y, int z) {
        // record
    }
}