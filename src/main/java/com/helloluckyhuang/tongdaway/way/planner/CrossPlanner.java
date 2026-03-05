package com.helloluckyhuang.tongdaway.way.planner;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.structure.ModStructureManager;
import com.helloluckyhuang.tongdaway.way.RegionPos;
import com.helloluckyhuang.tongdaway.structure.CrossTemplate;
import com.helloluckyhuang.tongdaway.util.MyMth;
import com.helloluckyhuang.tongdaway.util.MyRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static com.helloluckyhuang.tongdaway.TongDaWay.CHUNK_GROUP_SIZE;
import static com.helloluckyhuang.tongdaway.TongDaWay.HEIGHT_MAX_INCREMENT;


// 路口规划 连接规划
public class CrossPlanner {
    private final RegionPos regionPos;

    public CrossPlanner(RegionPos regionPos) {
        this.regionPos = regionPos;
    }

    // 区域内站点生成
    public static List<CrossGenInfo> generateCross(RegionPos regionPos, ServerLevel level, long seed) {
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState cfg = level.getChunkSource().randomState();

        long regionSeed = seed + regionPos.hashCode();
        List<CrossGenInfo> result = new ArrayList<>();
        int[] pos = MyRandom.generatePoints(regionSeed, CHUNK_GROUP_SIZE);
        ChunkPos chunkPos = new ChunkPos(MyMth.chunkPosXFromRegionPos(regionPos, pos[0]), MyMth.chunkPosZFromRegionPos(regionPos, pos[1]));

        int x = chunkPos.getBlockX(0);
        int z = chunkPos.getBlockZ(0);
        int y = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE, level, cfg);

        int h = y;

        // 使得站点的高度在一定区域内最小
        int miny = 2550;
        for (int ix = -2; ix < 3; ix++) {
            for (int iz = -2; iz < 3; iz++) {
                int ox = ix * 32 + x;
                int oz = iz * 32 + z;
                int ty = gen.getBaseHeight(ox, oz, Heightmap.Types.WORLD_SURFACE, level, cfg);
                miny = Math.min(miny, ty);
            }
        }

        if (y - miny > 20)
            h = miny;

        // 确保站点高度在 seaLevel ~ seaLevel + 增量
        h = Math.max(h, level.getSeaLevel());
        h = Math.min(h, level.getSeaLevel() + HEIGHT_MAX_INCREMENT);
        // 根据高度决定生成地上还是地下车站
        CrossTemplate cross;
        int placeH = h;
        if (h < y - 10) {
            cross = ModStructureManager.getRandomUnderGroundCross(regionSeed);
        } else {
            cross = ModStructureManager.getRandomNormalCross(regionSeed);
        }
        result.add(new CrossGenInfo(cross, new BlockPos(x, placeH, z)));

        return result;
    }

    // 路线连接规则生成
    public List<ConnectionGenInfo> generateConnections(ServerLevel level, long seed) {
        List<ConnectionGenInfo> result = new ArrayList<>();

        List<CrossGenInfo> thisStations = generateCross(regionPos, level, seed);

        List<CrossGenInfo> north = generateCross(new RegionPos(regionPos.x(), regionPos.z()-1), level, seed);
        List<CrossGenInfo> south = generateCross(new RegionPos(regionPos.x(), regionPos.z()+1), level, seed);

        List<CrossGenInfo> east = generateCross(new RegionPos(regionPos.x()+1, regionPos.z()), level, seed);
        List<CrossGenInfo> west = generateCross(new RegionPos(regionPos.x()-1, regionPos.z()), level, seed);

        var thisAssignedCross = assignCross(thisStations);

        var t = thisAssignedCross.getFirst();
        TongDaWay.LOGGER.info("====> StationPlanner: {} {} {} {}", (int)t.x, (int)t.y, (int)t.z, regionPos);

        var northAssignedExits = assignCross(north);
        var southAssignedExits = assignCross(south);
        var eastAssignedExits = assignCross(east);
        var westAssignedExits = assignCross(west);

        result.add(ConnectionGenInfo.getConnectionInfo(thisAssignedCross.get(3), eastAssignedExits.get(2)));
        result.add(ConnectionGenInfo.getConnectionInfo(westAssignedExits.get(3), thisAssignedCross.get(2)));
        result.add(ConnectionGenInfo.getConnectionInfo(northAssignedExits.get(1), thisAssignedCross.get(0)));
        result.add(ConnectionGenInfo.getConnectionInfo(thisAssignedCross.get(1), southAssignedExits.get(0)));

        return result;
    }

    /**
     * 分配东南西北向路口
     * @param cross 路口集合
     * @return 出口集合 顺序: [北, 南, 西, 东, 其他]
     */
    private List<Vec3> assignCross(List<CrossGenInfo> cross) {
        List<CrossGenInfo> copy = new ArrayList<>(cross);

        List<Vec3> result = new ArrayList<>();
        // 对出口的按z坐标进行排序 加偏防止z存在相同
        copy.sort(Comparator.comparingDouble(e -> {
            int z = e.placePos.getZ();
            Random random = new Random(75_1049 + z);
            double off = random.nextDouble() * 2 - 1;
            return z + off;
        }));

        result.add(copy.getFirst().placePos.getCenter()); //z最小 北
        result.add(copy.getLast().placePos.getCenter());  //z最大 南

        // 对出口的按x坐标进行排序 加偏防止x存在相同
        copy.sort(Comparator.comparingDouble(e -> {
            int x = e.placePos.getX();
            Random random = new Random(75_1052 + x);
            double off = random.nextDouble() * 2 - 1;
            return x + off;
        }));

        result.add(copy.getFirst().placePos.getCenter());  //x最小 西
        result.add(copy.getLast().placePos.getCenter());  //x最大 东

        return result; // 北 南 西 东
    }

    // 站点放置信息(世界坐标系)
    public record CrossGenInfo(
            CrossTemplate stationStructure,
            BlockPos placePos
    ) {
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("id", stationStructure.getId());
            tag.putInt("x", placePos.getX());
            tag.putInt("y", placePos.getY());
            tag.putInt("z", placePos.getZ());
            return tag;
        }

        public static CrossGenInfo fromNBT(CompoundTag tag) {
            int id = tag.getIntOr("id", 0);
            int x = tag.getIntOr("x", 0);
            int y = tag.getIntOr("y", 0);
            int z = tag.getIntOr("z", 0);
            CrossTemplate stationStructure = ModStructureManager.cross.getById(id);

            return new CrossGenInfo(stationStructure, new BlockPos(x, y, z));
        }
    }

    /**
     * @param start        起点坐标
     * @param end          终点坐标
     * @param connectStart 寻路起点
     * @param connectEnd   寻路终点
     */ // 路线连接信息(世界坐标系)
    public record ConnectionGenInfo(
            Vec3 start,
            Vec3 end,
            int[] connectStart,
            int[] connectEnd
    ) {
        public static ConnectionGenInfo getConnectionInfo(Vec3 A, Vec3 B) {
                Vec3 dir = B.subtract(A).normalize();
                int[] start = new int[] {
                        (int) A.add(dir.scale(30)).x,
                        (int) A.add(dir.scale(30)).z,
                        (int) A.y
                };
                int[] end = new int[] {
                        (int) B.add(dir.reverse().scale(30)).x,
                        (int) B.add(dir.reverse().scale(30)).z,
                        (int) B.y
                };

                return new ConnectionGenInfo(
                        A,
                        B,
                        start,
                        end
                );
            }
        }
}
