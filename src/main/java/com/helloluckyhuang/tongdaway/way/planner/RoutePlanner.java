package com.helloluckyhuang.tongdaway.way.planner;

import com.helloluckyhuang.tongdaway.util.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

import java.util.*;
import java.util.stream.Collectors;

import static com.helloluckyhuang.tongdaway.Tongdaway.HEIGHT_MAX_INCREMENT;


// 寻路 生成路径曲线
public class RoutePlanner {
    /**
     * 规划路径
     * @param way 路线图
     * @param connectionGenInfo 连接信息
     * @param world 服务器世界
     * @return 路径曲线和路线上点
     */
    public Pair<ResultWay, Set<int[]>> getWay(List<int[]> way, CrossPlanner.ConnectionGenInfo connectionGenInfo, WorldGenRegion world) {
        List<int[]> handledHeightWay = handleHeight(way, world.getLevel(), connectionGenInfo);
        return connectWay(world, handledHeightWay, connectionGenInfo);
    }

    /**
     * 测高 处理高度
     * @param path 直行路径(区域内坐标)
     * @param level 服务器世界
     * @param con 连接信息
     */
    public List<int[]> handleHeight(List<int[]> path, ServerLevel level, CrossPlanner.ConnectionGenInfo con) {
        List<double[]> adPath = new LinkedList<>();
        int seaLevel = level.getSeaLevel();

        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState cfg = level.getChunkSource().randomState();

        // 测高
        for (int[] p : path) {
            int wx = p[0];
            int wz = p[1];
            int h = gen.getBaseHeight(wx, wz, Heightmap.Types.WORLD_SURFACE_WG, level, cfg);

            // 限制高度范围
            h = Math.max(h, seaLevel);
            h = Math.min(h, seaLevel + HEIGHT_MAX_INCREMENT);
            adPath.add(new double[]{p[0], p[1], h});
        }

        adPath.get(0)[2] = con.connectStart()[2];
        adPath.get(adPath.size() - 1)[2] = con.connectEnd()[2];

        // 高度调整
        adPath = adjustmentHeight(adPath);

        //卷积平滑 保持首末点不变
        int max = adPath.stream().mapToInt(p -> (int) p[2]).max().orElse(0);
        int min = adPath.stream().mapToInt(p -> (int) p[2]).min().orElse(0);
        int framed2 = ((max - min) / (2*10)) + 1;


        if (adPath.size() > framed2*2 && framed2*2 >= 3) {
            // 平滑中间
            List<double[]> adPath1 = new ArrayList<>();
            adPath1.add(adPath.get(0));
            for (int i = 1; i < adPath.size()-1; i++) {
                double mean = 0;
                int sum = 0;
                for (int j = i-framed2; j <= i+framed2; j++) {
                    if (j >= 0 && j < adPath.size()) {
                        mean += adPath.get(j)[2];
                        sum++;
                    } else if (j < 0) {
                        mean += adPath.get(0)[2];
                        sum++;
                    } else {
                        mean += adPath.get(adPath.size() - 1)[2];
                        sum++;
                    }
                }
                mean /= sum;
                adPath1.add(new double[] {adPath.get(i)[0], adPath.get(i)[1], mean});
            }
            adPath1.add(adPath.get(adPath.size() - 1));
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
     * @param world 服务器世界
     * @param path 路线的端点
     * @param con 连接信息
     * @return 连接后的复合曲线和路线上点
     */
    private Pair<ResultWay, Set<int[]>> connectWay(WorldGenRegion world, List<int[]> path, CrossPlanner.ConnectionGenInfo con) {
        ServerLevel level = world.getLevel();
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState cfg = level.getChunkSource().randomState();

        // 转换为世界坐标系
        List<Vec3> path0 = new ArrayList<>();
        List<Boolean> isBridge = new ArrayList<>();
        List<int[]> used = new ArrayList<>();

        for (int i = 0; i < path.size() - 12; i+=3) {
            int[] point = path.get(i);
            path0.add(new Vec3(point[0], point[2], point[1]));
        }

        path0.add(new Vec3(path.get(path.size() - 1)[0], path.get(path.size() - 1)[2], path.get(path.size() - 1)[1]));

        for (Vec3 p : path0) {
            int h = gen.getBaseHeight((int) p.x, (int) p.z, Heightmap.Types.OCEAN_FLOOR_WG, level, cfg);
            var biome = BiomeGetter.getBiomeId(level, p);
            isBridge.add(!biome.contains("ocean") && (p.y - h > 5));
        }

        int n = isBridge.size();
        int m = 1;

        // ---------- 1. 删除长度 < 3 的 true 片段 ----------
        int i = 0;
        while (i < n) {
            if (!isBridge.get(i)) {
                i++;
                continue;
            }

            int start = i;
            while (i < n && isBridge.get(i)) {
                i++;
            }
            int end = i - 1;

            if (end - start + 1 < 3) {
                for (int j = start; j <= end; j++) {
                    isBridge.set(j, false);
                }
            }
        }

        // ---------- 2. 扩展 true 片段 ----------
        boolean[] mark = new boolean[n];

        i = 0;
        while (i < n) {
            if (!isBridge.get(i)) {
                i++;
                continue;
            }

            int start = i;
            while (i < n && isBridge.get(i)) {
                i++;
            }
            int end = i - 1;

            // 左侧扩展
            for (int j = 1; j <= m; j++) {
                int idx = start - j;
                if (idx >= 0 && !isBridge.get(idx)) {
                    mark[idx] = true;
                }
            }

            // 右侧扩展
            for (int j = 1; j <= m; j++) {
                int idx = end + j;
                if (idx < n && !isBridge.get(idx)) {
                    mark[idx] = true;
                }
            }
        }

        for (int j = 0; j < n; j++) {
            if (mark[j]) {
                isBridge.set(j, true);
            }
        }

        // 连接线路和车站
        String note = con.note();

        Vec3 first = path0.get(0);
        Vec3 last = path0.get(path0.size() - 1);

        ResultWay result = new ResultWay(new CurveRoute());

        // 路口起点连接
        result.addLine(level, con.start(), first, "normal", note);

        Vec3 startDir = first.subtract(con.start()).normalize();
        int ii = 0;
        while (ii < path0.size() - 1) {
            if (isBridge.get(ii)) {
                // 寻找片段终点
                int end = ii+1;
                while (end < path0.size() - 1 && isBridge.get(end)) {
                    end++;
                }
                result.addLine(level, path0.get(ii), path0.get(end), "bridge", note);
                ii = end;
                continue;
            }

            Vec3 endDir = (path0.get(ii).subtract(path0.get(ii+1))).normalize();

            result.addBezier(
                    level,
                    path0.get(ii),
                    startDir,
                    path0.get(ii+1).subtract(path0.get(ii)),
                    endDir,
                    "normal",
                    note
            );
            ii++;

            startDir = endDir.reverse();
        }

        // 终点路口连接
        result.addLine(level, last, con.end(), "normal", note);

        for (CurveRoute.CurveSegment segment : result.way.getSegments()) {
            if (segment instanceof CurveRoute.BezierSegment bez)
                used.add(new int[] {(int) bez.p0.x, (int) bez.p0.z, (int) bez.p0.y});
            else if (segment instanceof CurveRoute.LineSegment line)
                used.add(new int[] {(int) line.start.x, (int) line.start.z, (int) line.start.y});
        }

        return new Pair<>(result, new HashSet<>(used));
    }

    private static List<double[]> adjustmentHeight(List<double[]> path) {
        List<double[]> adjustedPath = new ArrayList<>();
        //连接首末点计算高度基线，求出相对高度。
        if (path.size() < 2)
            return new LinkedList<>();
        double hStart = path.get(0)[2];
        double hEnd = path.get(path.size() - 1)[2];
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
        public void addLine(ServerLevel level, Vec3 start, Vec3 end, String type, String note) {
            if ((start.subtract(end)).length() < 0.1)
                return;
            String biomeId = BiomeGetter.getBiomeId(level, start);
            way.addSegment(new CurveRoute.LineSegment(start, end, biomeId, type, note));
        }

        public void addBezier(ServerLevel level, Vec3 start, Vec3 startDir, Vec3 endOffset, Vec3 endDir, String type, String note) {
            if (endOffset.length() < 0.1)
                return;
            String biomeId = BiomeGetter.getBiomeId(level, start);
            if (Math.abs(startDir.dot(endDir)) > 0.9999 && startDir.dot(endOffset.normalize()) > 0.9999) {
                way.addSegment(new CurveRoute.LineSegment(start, start.add(endOffset), biomeId, type, note));
            } else {
                if (startDir.equals(new Vec3(0, 0, 0)))
                    startDir = endOffset.normalize();
                if (endDir.equals(new Vec3(0, 0, 0)))
                    endDir = endOffset.normalize().reverse();
                way.addSegment(CurveRoute.BezierSegment.getCubicBezier(start, startDir, endOffset, endDir, biomeId, type, note));
            }
        }
    }
}
