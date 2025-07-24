package com.hxzhitang.tongdaway.way;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

import static com.hxzhitang.tongdaway.Common.CHUNK_GROUP_SIZE;

public class RegionWayMap {
    // wayMaps 通过计算路线中每个点的区块坐标和计算高度，记录了区块组内所有区块的路线图
    // 路线生成时根据其中的坐标按规则直接生成路。
    private final Map<ChunkPos, List<WayPoint>> wayMaps = new HashMap<>();

    int x;
    int z;

    public RegionWayMap(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public List<WayPoint> getWayMap(ChunkPos chunkPos) {
        return wayMaps.get(chunkPos);
    }

    public void putWayMap(List<int[]> path, double[][] heightMap, int seaLevel, String wayName) {
        //高度平滑
        List<double[]> adjustedHeightMap = heightAdjustment(path, heightMap, seaLevel);
        //为区块生成路图
        for (double[] point : adjustedHeightMap) {
            int h = (int) point[2];
            int bx = (int) point[0];
            int bz = (int) point[1];
            int cx = ((int) Math.floor(bx / (double) 16)) + this.x * CHUNK_GROUP_SIZE;
            int cz = ((int) Math.floor(bz / (double) 16)) + this.z * CHUNK_GROUP_SIZE;
            ChunkPos chunkPos = new ChunkPos(cx, cz);
            List<WayPoint> chunkWayMap = getChunkWayMap(chunkPos);
            int ix = bx >= 0 ? bx % 16 : 15 + ((bx + 1) % 16);
            int iz = bz >= 0 ? bz % 16 : 15 + ((bz + 1) % 16);

            String type = "";
            int directionCode = (int) point[3];
            chunkWayMap.add(new WayPoint(new BlockPos(ix, h, iz), "way", directionCode, type));
        }

        //生成路灯图和路牌图
        int spacing = 24; //路灯(牌)生成间隔
        for (int i = 1; i < adjustedHeightMap.size() / spacing; i++) {
            double[] point = adjustedHeightMap.get(i*spacing);
            int bx = (int) point[0];
            int bz = (int) point[1];
            int h = (int) (point[2] + 1);
            int cx = ((int) Math.floor(bx / (double) 16)) + this.x * CHUNK_GROUP_SIZE;
            int cz = ((int) Math.floor(bz / (double) 16)) + this.z * CHUNK_GROUP_SIZE;
            ChunkPos chunkPos = new ChunkPos(cx, cz);
            List<WayPoint> chunkWayMap = getChunkWayMap(chunkPos);
            int ix = bx >= 0 ? bx % 16 : 15 + ((bx + 1) % 16);
            int iz = bz >= 0 ? bz % 16 : 15 + ((bz + 1) % 16);

            int directionCode = (int) point[3];

            //每8个灯生成一个路牌，或每条路开始生成一个路牌
            if (i % 8 == 0 || i == 1) {
                String newWayName = wayName;
                BlockPos pos = new BlockPos(ix, h, iz);

                Iterator<WayPoint> iterator = chunkWayMap.iterator();
                while(iterator.hasNext()) {
                    WayPoint p = iterator.next();
                    if (p.pos.equals(pos)) {
                        newWayName += ("\n" + p.pointNote);
                        iterator.remove();
                        break;
                    }
                }
                WayPoint wayPoint = new WayPoint(pos, "road_signs", directionCode, newWayName);
                chunkWayMap.add(wayPoint);
            } else
                chunkWayMap.add(new WayPoint(new BlockPos(ix, h, iz), "streetlight", directionCode, ""));
        }
    }

    public void putIntersection(WayMap wayMap) {
        int id = 0;
        for (BlockPos cross : wayMap.getIntersection().keySet()) {
            int x = cross.getX();
            int z = cross.getZ();
            int cx = ((int) Math.floor(cross.getX() / (double) 16));
            int cz = ((int) Math.floor(cross.getZ() / (double) 16));

            ChunkPos chunkPos = new ChunkPos(cx, cz);
            List<WayPoint> chunkWayMap = getChunkWayMap(chunkPos);
            int ix = x >= 0 ? x % 16 : 15 + ((x + 1) % 16);
            int iz = z >= 0 ? z % 16 : 15 + ((z + 1) % 16);
            int cy = -128;
            String note = wayMap.getIntersection().get(cross);
            chunkWayMap.add(new WayPoint(new BlockPos(ix, cy, iz), "intersection", id++, note));
        }
    }

    private List<WayPoint> getChunkWayMap(ChunkPos chunkPos) {
        List<WayPoint> chunkWayMap;
        if (wayMaps.containsKey(chunkPos)) {
            chunkWayMap = wayMaps.get(chunkPos);
        } else {
            chunkWayMap = new LinkedList<>();
            wayMaps.put(chunkPos, chunkWayMap);
        }
        return chunkWayMap;
    }

    //高度平滑方法
    //逢山开路遇水搭桥
    private static List<double[]> heightAdjustment(List<int[]> path, double[][] heightMap, int seaLevel) {
        List<double[]> adjustedHeightMap = new LinkedList<>();
        //连接首末点计算高度基线，求出相对高度。
        if (path.size() < 2)
            return adjustedHeightMap;
        double hStart = heightMap[path.getFirst()[0]][path.getFirst()[1]];
        double hEnd = heightMap[path.getLast()[0]][path.getLast()[1]];
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
        for (int i = 1; i < adjustedHeightMap.size() - 1; i++) {
            double[] p = adjustedHeightMap.get(i);
            double[] p0 = adjustedHeightMap.get(i-1);
            double[] p1 = adjustedHeightMap.get(i+1);
            p[2] = (p[2] + p0[2] + p1[2]) / 3;
        }

        return adjustedHeightMap;
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

    //编码Tag
    public ListTag getTag() {
        ListTag regionTag = new ListTag();
        //区域内区块
        for (ChunkPos chunkPos : wayMaps.keySet()) {
            //区块中路径
            ListTag wayListTag = new ListTag();
            CompoundTag chunkTag = new CompoundTag();
            for (WayPoint p : wayMaps.get(chunkPos)) {
                CompoundTag wayPointTag = new CompoundTag();
                wayPointTag.putInt("x", p.pos.getX());
                wayPointTag.putInt("y", p.pos.getY());
                wayPointTag.putInt("z", p.pos.getZ());
                wayPointTag.putString("type", p.pointType);
                wayPointTag.putInt("id", p.pointCode);
                wayPointTag.putString("note", p.pointNote);
                wayListTag.add(wayPointTag);
            }
            chunkTag.put("ChunkWay", wayListTag);
            chunkTag.putInt("ChunkX", chunkPos.x);
            chunkTag.putInt("ChunkZ", chunkPos.z);
            regionTag.add(chunkTag);
        }

        return regionTag;
    }

    //从Tag解码
    public static RegionWayMap loadTag(int regionX, int regionZ, ListTag listTag) {
        RegionWayMap regionWayMap = new RegionWayMap(regionX, regionZ);
        for (Tag tag : listTag) {
            CompoundTag chunkTag = (CompoundTag) tag;
            int chunkX = chunkTag.getInt("ChunkX");
            int chunkZ = chunkTag.getInt("ChunkZ");
            ListTag wayListTag = (ListTag) chunkTag.get("ChunkWay");
            if (wayListTag != null) {
                //区块中路径
                for (Tag wayTag : wayListTag) {
                    CompoundTag wayPointTag = (CompoundTag) wayTag;
                    int x = wayPointTag.getInt("x");
                    int y = wayPointTag.getInt("y");
                    int z = wayPointTag.getInt("z");
                    String type = wayPointTag.getString("type");
                    int id = wayPointTag.getInt("id");
                    String note = wayPointTag.getString("note");
                    regionWayMap.getChunkWayMap(new ChunkPos(chunkX, chunkZ)).add(new WayPoint(new BlockPos(x, y, z), type, id, note));
                }
            }
        }

        return regionWayMap;
    }

    // 此记录中的方块坐标为在一个区块内的区块坐标。高度为计算值。
    public record WayPoint(
            BlockPos pos,
            String pointType,
            int pointCode,
            String pointNote
    ) {
    }
}
