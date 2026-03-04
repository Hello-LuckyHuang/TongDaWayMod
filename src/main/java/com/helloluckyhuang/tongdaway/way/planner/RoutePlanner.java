package com.helloluckyhuang.tongdaway.way.planner;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.way.RailwayBuilder;
import com.helloluckyhuang.tongdaway.way.RegionPos;
import com.helloluckyhuang.tongdaway.util.AStarPathfinder;
import com.helloluckyhuang.tongdaway.util.AdaptiveHeightSampler;
import com.helloluckyhuang.tongdaway.util.CurveRoute;
import com.helloluckyhuang.tongdaway.util.MyMth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.helloluckyhuang.tongdaway.TongDaWay.CHUNK_GROUP_SIZE;
import static com.helloluckyhuang.tongdaway.TongDaWay.HEIGHT_MAX_INCREMENT;
import static com.helloluckyhuang.tongdaway.way.WayMap.samplingNum;


// 寻路 生成路径曲线
public class RoutePlanner {
    private final RegionPos regionPos;

    public RoutePlanner(RegionPos regionPos) {
        this.regionPos = regionPos;
    }

    // 获得十字形四向区域的损耗图
    public int[][] getCostMap(WorldGenRegion level) {
        int[][] heightMap = new int[CHUNK_GROUP_SIZE*samplingNum*3][CHUNK_GROUP_SIZE*samplingNum*3];
        for (int[] ints : heightMap) {
            Arrays.fill(ints, Integer.MAX_VALUE);
        }
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (Math.abs(i) == 1 && Math.abs(j) == 1)
                    continue;
                RegionPos rPos = new RegionPos(regionPos.x() + i, regionPos.z() + j);
                RailwayBuilder builder = RailwayBuilder.getInstance(level.getSeed());
                int[][] map;
                if (builder != null) {
                    map = builder.regionHeightMap
                            .computeIfAbsent(rPos, k -> getHeightMap(level.getLevel(), rPos));
                } else {
                    map = getHeightMap(level.getLevel(), rPos);
                }
                for (int x = 0; x < map.length; x++) {
                    for (int z = 0; z < map[0].length; z++) {
                        int picX = (i+1)*CHUNK_GROUP_SIZE*samplingNum+x;
                        int picZ = (j+1)*CHUNK_GROUP_SIZE*samplingNum+z;
                        heightMap[picX][picZ] = map[x][z];
                    }
                }
            }
        }

        return heightMap;
    }

    // 获得十字形四向区域的结构损耗图
    public int[][] getStructureCostMap(WorldGenRegion level) {
        int[][] structureMap = new int[CHUNK_GROUP_SIZE*samplingNum*3][CHUNK_GROUP_SIZE*samplingNum*3];
        for (int[] ints : structureMap) {
            Arrays.fill(ints, 50000);
        }
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (Math.abs(i) == 1 && Math.abs(j) == 1)
                    continue;
                RegionPos rPos = new RegionPos(regionPos.x() + i, regionPos.z() + j);
                RailwayBuilder builder = RailwayBuilder.getInstance(level.getSeed());
                int[][] map;
                if (builder != null) {
                    map = builder.regionStructureMap
                            .computeIfAbsent(rPos, k -> getStructureMap(level,rPos));
                } else {
                    map = getStructureMap(level,rPos);
                }
                for (int x = 0; x < map.length; x++) {
                    for (int z = 0; z < map[0].length; z++) {
                        int picX = (i+1)*CHUNK_GROUP_SIZE*samplingNum+x;
                        int picZ = (j+1)*CHUNK_GROUP_SIZE*samplingNum+z;
                        structureMap[picX][picZ] = map[x][z];
                    }
                }
            }
        }

        return structureMap;
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
            TongDaWay.LOGGER.info(" Build HeightMap time: {}ms", endTime - startTime);
        } catch (InterruptedException e) {
            TongDaWay.LOGGER.error("Build HeightMap Err", e);
        } finally {
            sampler.shutdown();
        }

        int[][] heightMap = sampler.generateImage(CHUNK_GROUP_SIZE*samplingNum, CHUNK_GROUP_SIZE*samplingNum);

        return heightMap;
    }

    private int[][] getStructureMap(WorldGenRegion level, RegionPos regionPos) {
        // 计算遗迹
        var serverLevel = level.getLevel();
        var registryAccess = level.registryAccess();
        var chunkGeneratorStructureState = serverLevel.getChunkSource().getGeneratorState();
        var structureManager = serverLevel.structureManager();
        var structureFeatureManager = serverLevel.getStructureManager();

        var dimensionType = level.dimensionType();
        LevelHeightAccessor levelHeightAccessor = LevelHeightAccessor.create(dimensionType.minY(), dimensionType.height());
        var biomeRegistry = registryAccess.lookupOrThrow(Registries.BIOME);

        List<BlockPos> structurePos = new ArrayList<>();

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
//                            var structureRegistry = registryAccess.registryOrThrow(Registries.STRUCTURE);
                            res.forEach((key, value) -> {
//                                String structureName = Objects.requireNonNull(structureRegistry.getKey(key)).toString();
                                BlockPos pos = new BlockPos(protoChunk.getPos().x * 16, 0, protoChunk.getPos().z * 16);
                                structurePos.add(pos);
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

        int[][] costMap = new int[CHUNK_GROUP_SIZE*samplingNum][CHUNK_GROUP_SIZE*samplingNum];
        for (BlockPos pos : structurePos) {
            int[] p = new int[] {
                    (pos.getX() - regionPos.x()*CHUNK_GROUP_SIZE*16)*samplingNum/16,
                    (pos.getZ() - regionPos.z()*CHUNK_GROUP_SIZE*16)*samplingNum/16
            };
            for (int x = -5*samplingNum; x < 5*samplingNum; x++) {
                for (int z = -5*samplingNum; z < 5*samplingNum; z++) {
                    int px = p[0]+x;
                    int pz = p[1]+z;
                    if (px > 0 && px < costMap.length && pz > 0 && pz < costMap[0].length)
                        costMap[px][pz] = 500;
                }
            }
        }

        return costMap;
    }

    /**
     * 规划路径
     * @param way 路线图
     */
    public ResultWay getWay(List<int[]> way, int[][] costMap, CrossPlanner.ConnectionGenInfo connectionGenInfo, WorldGenRegion world) {
        List<int[]> handledHeightWay = handleHeight(way, world.getLevel(), costMap, connectionGenInfo);
        // 结果转为中心图坐标系
        handledHeightWay = handledHeightWay.stream().map(AStarPathfinder::pic2RegionPos).toList();
        return connectWay(world, handledHeightWay, connectionGenInfo);
    }

    /**
     * 测高 处理高度
     * @param path 直行路径(区域内坐标)
     * @param level 服务器世界
     */
    public List<int[]> handleHeight(List<int[]> path, ServerLevel level, int[][] heightMap, CrossPlanner.ConnectionGenInfo con) {
        List<double[]> adPath = new LinkedList<>();
        int seaLevel = level.getSeaLevel();

        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState cfg = level.getChunkSource().randomState();

        // 测高
        for (int[] p : path) {
            int l = heightMap.length / 3;
            int wx = (int) ((p[0]-l)*(16.0/samplingNum) + regionPos.x()*CHUNK_GROUP_SIZE*16);
            int wz = (int) ((p[1]-l)*(16.0/samplingNum) + regionPos.z()*CHUNK_GROUP_SIZE*16);
            int h = gen.getBaseHeight(wx, wz, Heightmap.Types.WORLD_SURFACE_WG, level, cfg);

//            int h = heightMap[p[0]][p[1]];
            // 限制高度范围
            h = Math.max(h, seaLevel);
            h = Math.min(h, seaLevel + HEIGHT_MAX_INCREMENT);
            adPath.add(new double[]{p[0], p[1], h});
        }

        adPath.getFirst()[2] = con.connectStart()[2];
        adPath.getLast()[2] = con.connectEnd()[2];

        // 高度调整
        adPath = adjustmentHeight(adPath);

        //卷积平滑 保持首末点不变
        int max = adPath.stream().mapToInt(p -> (int) p[2]).max().orElse(0);
        int min = adPath.stream().mapToInt(p -> (int) p[2]).min().orElse(0);
        int framed2 = ((max - min) / (2*8)) + 1;


        if (adPath.size() > framed2*2 && framed2*2 >= 3) {
            // 平滑中间
            List<double[]> adPath1 = new ArrayList<>();
            adPath1.add(adPath.getFirst());
            for (int i = 1; i < adPath.size()-1; i++) {
                double mean = 0;
                int sum = 0;
                for (int j = i-framed2; j <= i+framed2; j++) {
                    if (j >= 0 && j < adPath.size()) {
                        mean += adPath.get(j)[2];
                        sum++;
                    } else if (j < 0) {
                        mean += adPath.getFirst()[2];
                        sum++;
                    } else {
                        mean += adPath.getLast()[2];
                        sum++;
                    }
                }
                mean /= sum;
                adPath1.add(new double[] {adPath.get(i)[0], adPath.get(i)[1], mean});
            }
            adPath1.add(adPath.getLast());
            adPath = adPath1;

            // 平滑起末
            double fh = con.connectStart()[2];
            double lh = con.connectEnd()[2];
            if (adPath.size() > framed2*2+20) {
                for (int i = 1; i < framed2+10; i++) {
                    double t = (double) i / (framed2+10);
                    double sh = adPath.get(i)[2];
                    double eh = adPath.get(adPath.size() - 1 - i)[2];

                    adPath.get(i)[2] = fh * (1 - t) + sh * t;
                    adPath.get(adPath.size() - 1 - i)[2] = lh * (1 - t) + eh * t;
                }
            }
        }

        return adPath.stream()
                .map(arr -> Arrays.stream(arr)
                        .mapToInt(d -> (int) Math.round(d))  // 四舍五入
                        .toArray()
                )
                .collect(Collectors.toList());
    }

    /**
     * 将直线路径段通过三阶贝塞尔曲线平滑连接
     * @param path 路线的端点
     * @return 连接后的复合曲线
     */
    private ResultWay connectWay(WorldGenRegion world, List<int[]> path, CrossPlanner.ConnectionGenInfo con) {
        ServerLevel level = world.getLevel();
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState cfg = level.getChunkSource().randomState();

        // 转换为世界坐标系
        List<Vec3> path0 = new ArrayList<>();
        List<Boolean> isBridge = new ArrayList<>();

        for (int i = 0; i < path.size() - 12; i+=3) {
            int[] point = path.get(i);
            path0.add(MyMth.inRegionPos2WorldPos(
                    regionPos,
                    new Vec3(point[0], point[2], point[1])
                            .multiply(16.0/samplingNum, 1, 16.0/samplingNum)
            ));
        }

        path0.addLast(MyMth.inRegionPos2WorldPos(
                regionPos,
                new Vec3(path.getLast()[0], path.getLast()[2], path.getLast()[1])
                        .multiply(16.0/samplingNum, 1, 16.0/samplingNum)
        ));

        var a = path0.getLast();
        System.out.println((int) a.x + " " + (int) a.y + " " + (int) a.z);
        System.out.println("=======>>>> "+level.getNoiseBiome((int) a.x/4, (int) a.y, (int) a.z/4));

        for (Vec3 p : path0) {
            int h = gen.getBaseHeight((int) p.x, (int) p.z, Heightmap.Types.WORLD_SURFACE_WG, level, cfg);
            if (h - p.y > 5) {
                isBridge.add(true);
            } else {
                isBridge.add(false);
            }
        }

        // 连接线路和车站
        Vec3 first = path0.getFirst();
        Vec3 last = path0.getLast();

        ResultWay result = new ResultWay(new CurveRoute());

        // 车站起点连接
        result.addLine(con.start(), first, "", "");

        Vec3 startDir = first.subtract(con.start()).normalize();
        int i = 0;
        while (i < path0.size() - 1) {
            Vec3 endDir = (path0.get(i).subtract(path0.get(i+1))).normalize();
            result.addBezier(
                    path0.get(i),
                    startDir,
                    path0.get(i+1).subtract(path0.get(i)),
                    endDir,
                    "",
                    ""
            );
            i++;

            startDir = endDir.reverse();
        }

        // 终点车站连接
        result.addLine(last, con.end(), "", "");

        return result;
    }

    private static List<double[]> adjustmentHeight(List<double[]> path) {
        List<double[]> adjustedPath = new ArrayList<>();
        //连接首末点计算高度基线，求出相对高度。
        if (path.size() < 2)
            return new LinkedList<>();
        double hStart = path.getFirst()[2];
        double hEnd = path.getLast()[2];
        double pNum = path.size() - 1;

        //计算相对高度
        List<double[]> heightList0 = new ArrayList<>(); //坐标、相对高度
        Map<Integer, List<double[]>> heightGroups = new HashMap<>(); //高度索引表
        double distance = 0;
        for (int i = 0; i < path.size(); i++) {
            //计算相对高度
            double[] point = path.get(i);
            double h = point[2] - hStart * ((pNum - i) / pNum) - hEnd * (i / pNum);
            //计算距离
            if (i > 0) {
                double h0 = point[2];
                double h1 = path.get(i-1)[2];
                distance += 1 + Math.abs(h0 - h1);
            }
            //生成点
            double[] p = {point[0], point[1], h, i, distance}; //x,z,高度,索引,距离
            //添加到点表
            heightList0.add(p);
            //添加高度索引表
            int hi = (int) h;
            heightGroups.computeIfAbsent(hi, k -> new ArrayList<>()).add(p);
        }
        // 三角函数
        double sec = Math.sqrt(Math.pow(heightList0.size(), 2) + Math.pow(Math.abs(hStart - hEnd), 2)) / (heightList0.size());

        //削峰填谷
        for (int j = 0; j < heightList0.size(); j++) {
            double[] thisPoint = heightList0.get(j); //获取目前点
            adjustedPath.add(new double[] {thisPoint[0], thisPoint[1], thisPoint[2]});
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
                boolean conditionBridge = hd < 0 && (iB - iA) * 8 * sec < dB - dA;
                boolean conditionTunnel = hd > 0 && (iB - iA) * 6 * sec < dB - dA;
                if (conditionBridge || conditionTunnel) {
                    //调整高度
                    for (int k = j; k < nextPointIndex; k++) {
                        double[] np1 = heightList0.get(k+1);
                        adjustedPath.add(new double[] {np1[0], np1[1], thisPoint[2]});
                    }
                    j = nextPointIndex;
                }
            }
        }
        //最终再将所有点的高度加上基线
        for (int i = 0; i < adjustedPath.size(); i++) {
            double[] p = adjustedPath.get(i);
            p[2] += hStart * ((pNum - i) / pNum) + hEnd * (i / pNum);
        }

        return adjustedPath;
    }

    public record ResultWay(
            CurveRoute way
    ) {
        public void addLine(Vec3 start, Vec3 end, String biome, String type) {
            way.addSegment(new CurveRoute.LineSegment(start, end, biome, type));
        }

        public void addBezier(Vec3 start, Vec3 startDir, Vec3 endOffset, Vec3 endDir, String biome, String type) {
            if (Math.abs(startDir.dot(endDir)) > 0.9999 && startDir.dot(endOffset.normalize()) > 0.9999) {
                way.addSegment(new CurveRoute.LineSegment(start, start.add(endOffset), biome, type));
            } else {
                way.addSegment(new CurveRoute.BezierSegment(start, startDir, endOffset, endDir, biome, type));
            }
        }
    }
}
