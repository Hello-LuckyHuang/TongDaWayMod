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

    public void putWayMap(List<int[]> path, double[][] heightMap, String wayName) {
        //高度平滑
        List<double[]> adjustedHeightMap = heightAdjustment(path, heightMap);
        //为区块生成路图
        int id = 0;
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
            int cy = -128;
//            if (bx % blockStride == 0 && bz % blockStride == 0)
//                cy = heightMap[bx/blockStride][bz/blockStride];
            cy = (int) heightMap[bx][bz];

            int changed = (int) point[3];
            String type = "";
            if (changed == 1)
                type = "bridge";
            chunkWayMap.add(new WayPoint(new BlockPos(ix, h, iz), "way", id++, type));
        }

        //生成路灯图和路牌图
        int spacing = 24; //路灯(牌)生成间隔
        for (int i = 1; i < path.size() / spacing; i++) {
            int[] point = path.get(i*spacing);
            int bx = point[0];
            int bz = point[1];
            int cx = ((int) Math.floor(bx / (double) 16)) + this.x * CHUNK_GROUP_SIZE;
            int cz = ((int) Math.floor(bz / (double) 16)) + this.z * CHUNK_GROUP_SIZE;
            ChunkPos chunkPos = new ChunkPos(cx, cz);
            List<WayPoint> chunkWayMap = getChunkWayMap(chunkPos);
            int ix = bx >= 0 ? bx % 16 : 15 + ((bx + 1) % 16);
            int iz = bz >= 0 ? bz % 16 : 15 + ((bz + 1) % 16);

            //每8个灯生成一个路牌，或每条路开始生成一个路牌
            if (i % 8 == 0 || i == 1) {
                String newWayName = wayName;
                BlockPos pos = new BlockPos(ix, -128, iz);

                Iterator<WayPoint> iterator = chunkWayMap.iterator();
                while(iterator.hasNext()) {
                    WayPoint p = iterator.next();
                    if (p.pos.equals(pos)) {
                        newWayName += ("\n" + p.pointNote);
                        iterator.remove();
                        break;
                    }
                }
                WayPoint wayPoint = new WayPoint(pos, "road_signs", i-1, newWayName);
                chunkWayMap.add(wayPoint);
            } else
                chunkWayMap.add(new WayPoint(new BlockPos(ix, -128, iz), "streetlight", i-1, ""));
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
    //逢山开路遇水搭桥(暂时不可用)
    private static List<double[]> heightAdjustment(List<int[]> path, double[][] heightMap) {
        List<double[]> adjustedHeightMap = new LinkedList<>();
        //记录每个点的高度，然后进行处理
        List<double[]> heightList = new ArrayList<>();
        //连接首末点计算高度基线，求出相对高度。
        if (path.size() < 2)
            return adjustedHeightMap;
        double hStart = heightMap[path.get(0)[0]][path.get(0)[1]];
        double hEnd = heightMap[path.get(path.size() - 1)[0]][path.get(path.size() - 1)[1]];
        hStart = hStart < 63 ? 63 : hStart;
        hEnd = hEnd < 63 ? 63 : hEnd;
        double pNum = path.size() - 1;
        int i = 0;
        //计算相对高度
        for (int[] point : path) {
            int bx = point[0];
            int bz = point[1];
            double h = heightMap[bx][bz] - hStart * ((pNum - i) / pNum) - hEnd * (i / pNum);
            double[] p = {bx, bz, h, 0};
            heightList.add(p);
            adjustedHeightMap.add(p);
            i++;
        }
        //对相对高度排序
        heightList.sort(Comparator.comparingDouble(p -> p[2]));
        //如果长度过短，则不处理。
        if (path.size() >= 100) {
            //从小值(谷)开始，抹去谷的高度。
            //相对基线最小值大于-5则表明已抹去全部谷。
            while (!heightList.isEmpty() && heightList.get(0)[2] < -5) {
                //查找谷
                Set<double[]> temp = new HashSet<>(); //注意去重
                //向后查找直到相对高度大于上限
                int upperLimit = 0;
                double deepest = heightList.get(0)[2];
                for (int b = adjustedHeightMap.indexOf(heightList.get(0)); b < adjustedHeightMap.size(); b++) {
                    double[] p = adjustedHeightMap.get(b);
                    if (!(p[2] >= upperLimit)) {
                        temp.add(p);
                    } else {
                        break;
                    }
                }
                //向前查找直到相对高度大于上限
                for (int f = adjustedHeightMap.indexOf(heightList.get(0)); f >= 0; f--) {
                    double[] p = adjustedHeightMap.get(f);
                    if (!(p[2] >= upperLimit)) {
                        temp.add(p);
                    } else {
                        break;
                    }
                }
                //判断条件:1.谷的宽高比 2.谷宽 抹去谷的高度
                int width = temp.size();
                if (!(deepest/width < 0.866 && (width > 20 || width < 3))) {
                    //“架桥”
                    for (double[] p : temp) {
                        int j = adjustedHeightMap.indexOf(p);
                        adjustedHeightMap.get(j)[2] = 0;
                        adjustedHeightMap.get(j)[3] = 1;
                    }
                }
                //对于此谷处理结束，从待处理点中剔除处理完毕的点。
                for (double[] p : temp) {
                    heightList.remove(p);
                }
            }
        }
        //最终再将所有点的高度加上基线
        for (double[] p : adjustedHeightMap) {
            p[2] += hStart * ((pNum - adjustedHeightMap.indexOf(p)) / pNum) + hEnd * (adjustedHeightMap.indexOf(p) / pNum);
        }

        return adjustedHeightMap;
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
                wayPointTag.putInt("id", p.pointId);
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
            int pointId,
            String pointNote
    ) {
    }
}
