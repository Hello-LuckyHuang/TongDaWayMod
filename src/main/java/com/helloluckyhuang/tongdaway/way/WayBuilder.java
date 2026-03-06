package com.helloluckyhuang.tongdaway.way;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.util.SaveWayData;
import net.minecraft.server.level.WorldGenRegion;

import java.util.Map;
import java.util.concurrent.*;

public class WayBuilder {
    private static WayBuilder instance;
    private static long seed;

    private final Map<RegionPos, Future<?>> regionFutures = new ConcurrentHashMap<>();
    public final Map<RegionPos, WayMap> regionRailways = new ConcurrentHashMap<>();
    public final Map<RegionPos, int[][]> regionHeightMap = new ConcurrentHashMap<>();
    public final Map<RegionPos, int[][]> regionStructureMap = new ConcurrentHashMap<>();

    private final LinkedBlockingQueue<Runnable> regionRailwayLoadQueue = new LinkedBlockingQueue<Runnable>(); //线程池
    private final ThreadPoolExecutor regionRailwayLoadPoolExecutor = new ThreadPoolExecutor(64, 1024, 1, TimeUnit.DAYS, regionRailwayLoadQueue);
    private final WorldGenRegion level;

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
        if (regionRailways.containsKey(regionPos)) {
            return;
        }

        // 尝试从本地数据中读取
        WayMap savedData = SaveWayData.getWayMap(regionPos, level.getServer());
        if (savedData != null) {
            regionRailways.put(regionPos, savedData);
            TongDaWay.LOGGER.info("Region {} Done! Read From Local Data", regionPos);
            return;
        }

        // 如果路线还未生成...
        try {
            // 如果没有线程在生成路线，添加线程开始生成
            if (!regionFutures.containsKey(regionPos)) {
                var f = regionRailwayLoadPoolExecutor.submit(() -> {
                    // 生成铁路步骤...
                    WayMap wayMap = new WayMap(regionPos);

                    wayMap.startPlanningRoutes(level);

                    // 放置路线规划结果
                    regionRailways.put(regionPos, wayMap);

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
}
