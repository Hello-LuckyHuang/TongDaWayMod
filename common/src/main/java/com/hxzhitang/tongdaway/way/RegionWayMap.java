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

    public void putWayMap(List<int[]> path, String wayName) {
        //为区块生成路图
        for (int[] point : path) {
            int h = point[2];
            int bx = point[0];
            int bz = point[1];
            int cx = ((int) Math.floor(bx / (double) 16)) + this.x * CHUNK_GROUP_SIZE;
            int cz = ((int) Math.floor(bz / (double) 16)) + this.z * CHUNK_GROUP_SIZE;
            ChunkPos chunkPos = new ChunkPos(cx, cz);
            List<WayPoint> chunkWayMap = getChunkWayMap(chunkPos);
            int ix = bx >= 0 ? bx % 16 : 15 + ((bx + 1) % 16);
            int iz = bz >= 0 ? bz % 16 : 15 + ((bz + 1) % 16);

            String type = "";
            int directionCode = point[3];
            chunkWayMap.add(new WayPoint(new BlockPos(ix, h, iz), "way", directionCode, type));
        }

        //生成路灯图和路牌图
        int spacing = 24; //路灯(牌)生成间隔
        for (int i = 1; i < path.size() / spacing; i++) {
            int[] point = path.get(i*spacing);
            int bx = point[0];
            int bz = point[1];
            int h = point[2] + 1;
            int cx = ((int) Math.floor(bx / (double) 16)) + this.x * CHUNK_GROUP_SIZE;
            int cz = ((int) Math.floor(bz / (double) 16)) + this.z * CHUNK_GROUP_SIZE;
            ChunkPos chunkPos = new ChunkPos(cx, cz);
            List<WayPoint> chunkWayMap = getChunkWayMap(chunkPos);
            int ix = bx >= 0 ? bx % 16 : 15 + ((bx + 1) % 16);
            int iz = bz >= 0 ? bz % 16 : 15 + ((bz + 1) % 16);

            int directionCode = point[3];

            // 生成桥墩
//            chunkWayMap.add(new WayPoint(new BlockPos(ix, h, iz), "pier", directionCode, ""));

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

    protected record PointPos(int x, int z) {
    }
}
