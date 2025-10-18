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
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.*;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        int maxHeight = worldGenRegion.getMaxBuildHeight() - 176 + 2;

        LevelHeightAccessor levelHeightAccessor = LevelHeightAccessor.create(dimensionType.minY(), dimensionType.height());

        var serverLevel = worldGenRegion.getLevel();
        var registryAccess = worldGenRegion.registryAccess();
        var chunkGeneratorStructureState = serverLevel.getChunkSource().getGeneratorState();
        var structureManager = serverLevel.structureManager();
        var structureFeatureManager = serverLevel.getStructureManager();

        var biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);

        int picLength = CHUNK_GROUP_SIZE * (16/blockStride);

        int[][] heightMap = new int[picLength][picLength];

        //生成路径图
        WayMap wayMap = new WayMap(this.x, this.z, this.seed);

        for (int gx = 0; gx < CHUNK_GROUP_SIZE; gx++) {
            for (int gz = 0; gz < CHUNK_GROUP_SIZE; gz++) {
                var protoChunk = new ProtoChunk(new ChunkPos(x * CHUNK_GROUP_SIZE + gx, z * CHUNK_GROUP_SIZE + gz), UpgradeData.EMPTY, levelHeightAccessor, biomeRegistry, null);
                if (protoChunk.getStatus() == ChunkStatus.EMPTY) {
                    int finalGx = gx;
                    int finalGz = gz;

                    heightMapLoadPoolExecutor.submit(() -> {
                        //计算高度图
                        var heightMapTile = getHeight(protoChunk.getPos(), blockStride, seaLevel, maxHeight);
                        for (int px = 0; px < 16/blockStride; px++) {
                            for (int pz = 0; pz < 16 / blockStride; pz++) {
                                int x = finalGx * 16 / blockStride + px;
                                int z = finalGz * 16 / blockStride + pz;
                                int y = heightMapTile[px][pz].y;
                                heightMap[x][z] = Math.min(y, maxHeight);
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
        RegionWayMap regionWayMap = new RegionWayMap(this.x, this.z);
        double[][] expandedHeightMap = ExpandImage.expandImage(heightMap, blockStride);
        double[][] slopeImg = ImageGradient.calculateGradient(expandedHeightMap);
        wayMap.applyStructureNode(); //生成遗迹路线
        List<WayPath> paths = new ArrayList<>();
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
            List<int[]> path = OptimizedAStarEightDirections.findMinimumCostPath(slopeImg, start, end, (x, y) -> {
                final int h = heightMap[x/blockStride][y/blockStride];
                return h <= seaLevel || h >= maxHeight-2 ? 5000.0 : 0.0;
            });
            // 标记已生成路径，尽量避免路径重合
//            for (int[] pp : path) {
//                slopeImg[pp[0]][pp[1]] = 10000;
//            }
            //输入计算路径和原计算高度图（非插值后）,为路线生成提供依据
            paths.add(new WayPath(path, wayName));
        }
        // 合并路径，避免路径重合
        var mergedPaths = mergeWay(paths);
        // 调整高度
        for (var path : mergedPaths) {
            path.points = heightAdjustment(path.points, expandedHeightMap, seaLevel);
        }
        // 调整连接点
        nodeHeightAdjustment(mergedPaths);
        // 生成路图
        for (var path : mergedPaths) {
            regionWayMap.putWayMap(path.points, path.wayName);
        }
        regionWayMap.putIntersection(wayMap); // 将路口加入到regionWayMap中
        chunkWays.put(new RegionPos(this.x, this.z), regionWayMap);
        //将数据保存到磁盘
        data.putRegionWay(new RegionPos(this.x, this.z), regionWayMap);

        long duration = System.currentTimeMillis() - startTime;
//        Common.LOGGER.info("Region {},{} Done! Way Nodes Num: {}, Quick Finish: {}, Use Time: {} ms", this.x, this.z, nodeNum, quickLoadHeightMap, duration);
    }

    // 测试样例
    // -17109142 324 256 924
    /**
     * 预获取高度图
     * 此部分计算地表高度代码使用自：Taiterio的world preview
     * 原始许可证：Apache 2.0协议
     * 原始代码链接：https://github.com/caeruleusDraconis/world-preview
     * 源方法：caeruleusTait.world.preview.backend.worker.HeightmapWorkUnit.doWork
     * 本方法修改后适应本mod的区块高度图计算
     * @param chunkPos 区块坐标
     * @param blockStride 采样步长，要16的约数
     * @param seaLevel 海平面高度
     * @param maxHeight 最大高度
     * @return 高度图
     */
    private HeightData[][] getHeight(ChunkPos chunkPos, int blockStride, int seaLevel, int maxHeight) {
        final NoiseSettings noiseSettings = noiseGeneratorSettings.noiseSettings();
        final NoiseChunk noiseChunk = getNoiseChunkFunc.getNoiseChunk(chunkPos, random);
        final Predicate<BlockState> predicate = Heightmap.Types.OCEAN_FLOOR_WG.isOpaque();

        final int cellWidth = noiseSettings.getCellWidth();
        final int cellHeight = noiseSettings.getCellHeight();
        final int minY = noiseSettings.minY();
        final int maxY = maxHeight + 20;//minY + noiseSettings.height();
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

    //同路合并方法：将几个路合并拆分为数个不重合短路，确保相同xz的点一定只有一条路占用
    protected static List<WayPath> mergeWay(List<WayPath> paths) {
        // 测试样例
        // 1437601962791039007
        // -360 123 -1096
        Set<RegionWayMap.PointPos> set = new HashSet<>(); // 已处理的点
        List<WayPath> newWays = new ArrayList<>(); // 新的路列表
        // 将路根据重复的点切分成数个短路。重复放点跳过，只被一条路使用。
        paths.sort(Comparator.comparingInt(o -> o.points.size()));
        for (WayPath path : paths) {
            newWays.add(new WayPath(new ArrayList<>(), path.wayName)); // 新建一条短路
            for (int[] point : path.points) {
                var pos = new RegionWayMap.PointPos(point[0], point[1]);
                if (set.contains(pos)) {
                    if (!newWays.get(newWays.size() - 1).points.isEmpty()) {
                        // 上一条路被重复点截断，新建一条短路
                        newWays.add(new WayPath(new ArrayList<>(), path.wayName));
                    }
                    continue;
                }
                set.add(pos); // 标记为已处理
                newWays.get(newWays.size() - 1).points.add(new int[]{point[0], point[1]});
            }
        }
        newWays.removeIf(WayPath::isEmpty); // 移除可能的空列表
        return newWays;
    }

    //高度平滑方法
    //逢山开路遇水搭桥
    private static List<int[]> heightAdjustment(List<int[]> path, double[][] heightMap, int seaLevel) {
        List<double[]> adjustedHeightMap = new LinkedList<>();
        //连接首末点计算高度基线，求出相对高度。
        if (path.size() < 2)
            return new LinkedList<>();
        double hStart = heightMap[path.get(0)[0]][path.get(0)[1]];
        double hEnd = heightMap[path.get(path.size() - 1)[0]][path.get(path.size() - 1)[1]];
        hStart = hStart < seaLevel ? seaLevel : hStart;
        hEnd = hEnd < seaLevel ? seaLevel : hEnd;
        double pNum = path.size() - 1;

        //计算相对高度
        List<double[]> heightList0 = new ArrayList<>(); //坐标、相对高度
        Map<Integer, List<double[]>> heightGroups = new HashMap<>(); //高度索引表
        double distance = 0;
        for (int i = 0; i < path.size(); i++) {
            //计算相对高度
            int[] point = path.get(i);
            int bx = point[0];
            int bz = point[1];
            double h = heightMap[bx][bz] - hStart * ((pNum - i) / pNum) - hEnd * (i / pNum);
            //计算距离
            if (i > 0) {
                double h0 = heightMap[bx][bz];
                double h1 = heightMap[path.get(i-1)[0]][path.get(i-1)[1]];
                distance += 1 + Math.abs(h0 - h1);
            }
            //生成点
            double[] p = {bx, bz, h, i, distance}; //x,z,高度,索引,距离
            //添加到点表
            heightList0.add(p);
            //添加高度索引表
            int hi = (int) h;
            heightGroups.computeIfAbsent(hi, k -> new ArrayList<>()).add(p);
        }
        double sec = Math.sqrt(Math.pow(heightList0.size(), 2) + Math.pow(Math.abs(hStart - hEnd), 2)) / (heightList0.size());

        //削峰填谷
        for (int j = 0; j < heightList0.size(); j++) {
            double[] thisPoint = heightList0.get(j); //获取目前点
            adjustedHeightMap.add(new double[] {thisPoint[0], thisPoint[1], thisPoint[2], 0});
            int hd = 0; //hd: 下一个点和目前点的高差
            if (j < heightList0.size() - 1) { //下一个点和目前点的高差
                hd = (int)heightList0.get(j+1)[2] - (int)thisPoint[2];
            }
            //同高度，跳过
            if (hd == 0)
                continue;
            double h = thisPoint[2]; //目前点高度
            var group = heightGroups.get((int)h); //获取目前点同高度的点组
            int groupIndex = group.indexOf(thisPoint); //当前点在点组中的索引
            //获取同高度点组中的下一个点
            if (groupIndex < group.size() - 1) { //如果有后继
                double[] nextSameHeightPoint = group.get(groupIndex+1); //同高度的下一个点
                int nextPointIndex = heightList0.indexOf(nextSameHeightPoint); //它的索引
                double dA = thisPoint[4], dB = nextSameHeightPoint[4];
                double iA = thisPoint[3], iB = nextSameHeightPoint[3];
                //可能的桥 可能的隧道
                if (
                        hd < 0 && (iB - iA)*3*sec < dB - dA ||
                                hd > 0 && (iB - iA)*2*sec < dB - dA
                ) {
                    //调整高度
                    for (int k = j; k < nextPointIndex; k++) {
                        double[] np1 = heightList0.get(k+1);
                        adjustedHeightMap.add(new double[] {np1[0], np1[1], thisPoint[2], (np1[2]-thisPoint[2])});
                    }
                    j = nextPointIndex;
                }
            }
        }
        //最终再将所有点的高度加上基线
        for (int i = 0; i < adjustedHeightMap.size(); i++) {
            double[] p = adjustedHeightMap.get(i);
            p[2] += hStart * ((pNum - i) / pNum) + hEnd * (i / pNum);
            p[2] = p[2] <= seaLevel ? seaLevel-1 : p[2];
        }
        //计算方向编码
        for (int i = 0; i < adjustedHeightMap.size(); i++) {
            int direction = getDirection(i, heightList0, adjustedHeightMap);
            adjustedHeightMap.get(i)[3] = direction;
        }
        //卷积平滑
        int max = adjustedHeightMap.stream().mapToInt(p -> (int) p[2]).max().orElse(0);
        int min = adjustedHeightMap.stream().mapToInt(p -> (int) p[2]).min().orElse(0);
        int framed2 = ((max - min) / 2) + 10;
        if (adjustedHeightMap.size() > framed2*2 && framed2*2 >= 3) {
            List<double[]> adjustedHeightMap0 = new ArrayList<>();
            for (int i = 0; i < adjustedHeightMap.size(); i++) {
                double mean = 0;
                int sum = 0;
                for (int j = i-framed2; j <= i+framed2; j++) {
                    if (j >= 0 && j < adjustedHeightMap.size()) {
                        mean += adjustedHeightMap.get(j)[2];
                        sum++;
                    }
                }
                mean /= sum;
                double k = 1;
                if (i < framed2) {
                    k = i / ((double) framed2);
                } else if (i > adjustedHeightMap.size() - framed2 - 1) {
                    k = (adjustedHeightMap.size() - i - 1) / ((double) framed2);
                }
                double result = mean * k + adjustedHeightMap.get(i)[2] * (1 - k);
                adjustedHeightMap0.add(new double[] {adjustedHeightMap.get(i)[0], adjustedHeightMap.get(i)[1], result, adjustedHeightMap.get(i)[3]});
            }
            adjustedHeightMap = adjustedHeightMap0;
        }

        return adjustedHeightMap.stream()
                .map(arr -> Arrays.stream(arr)
                        .mapToInt(d -> (int) Math.round(d))  // 四舍五入
                        .toArray()
                )
                .collect(Collectors.toList());
    }

    //连接点平整 保证路的连接点高度一致
    private static void nodeHeightAdjustment(List<WayPath> ways) {
        for (int i = 0; i < ways.size() - 1; i++) {
            WayPath path = ways.get(i);
            for (int[] p : path.points) {
                for (int j = i+1; j < ways.size(); j++) {
                    WayPath nextPath = ways.get(j);
                    if (nextPath.points.size() > 5) {
                        int[] ps = nextPath.points.get(0);
                        int[] pe = nextPath.points.get(nextPath.points.size() - 1);
                        int h = p[2];
                        int nl = Math.min(nextPath.points.size(), 10);
                        if (Math.abs(p[0] - ps[0])+Math.abs(p[1]-ps[1]) <= 1) {
                            int h0 = nextPath.points.get(nl-1)[2];
                            for (int k = 0; k < nl; k++) {
                                float f = (float) k / (nl-1);
                                nextPath.points.get(k)[2] = (int) ((h0*f)+(h*(1-f)));
                            }
                        } else if (Math.abs(p[0] - pe[0])+Math.abs(p[1]-pe[1]) <= 1) {
                            int h0 = nextPath.points.get(nextPath.points.size()-nl)[2];
                            for (int k = 0; k < nl; k++) {
                                float f = (float) k / (nl-1);
                                nextPath.points.get(nextPath.points.size() - 1 - k)[2] = (int) ((h0*f)+(h*(1-f)));
                            }
                        }
                    }
                }
            }
        }
    }

    private static int getDirection(int i, List<double[]> heightList0, List<double[]> adjustedHeightMap) {
        int direction = 0;
        if (i < heightList0.size() - 1) {
            direction = getDirectionCode(
                    (int) adjustedHeightMap.get(i)[0], (int) adjustedHeightMap.get(i)[1],
                    (int) adjustedHeightMap.get(i +1)[0], (int) adjustedHeightMap.get(i +1)[1]
            );
        } else {
            direction = getDirectionCode(
                    (int) heightList0.get(i -1)[0], (int) heightList0.get(i -1)[1],
                    (int) heightList0.get(i)[0], (int) heightList0.get(i)[1]
            );
        }
        return direction;
    }

    private static int getDirectionCode(int bx, int bz, int nx, int nz) {
        //方向编码
        int dx = nx == bx ? 1 : nx > bx ? 2 : 0;
        int dz = nz == bz ? 1 : nz > bz ? 2 : 0;

        return dz * 3 + dx;
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