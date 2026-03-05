package com.helloluckyhuang.tongdaway.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MyRandom {
    
    /**
     * 在[0, range]范围内随机生成一个或两个相距不小于range/3的xz坐标点
     *
     * @param seed  随机数种子
     * @param range 坐标范围
     * @return 包含xz坐标点的int数组，每个点用连续两个元素表示(x, z)
     */
    public static int[] generatePoints000(long seed, int range) {
        Random random = new Random(seed);

        int quart = range / 4;

        // 先生成一个点
        int x1 = random.nextInt(quart, range - quart);
        int z1 = random.nextInt(quart, range - quart);

        return new int[]{x1, z1};
    }

    /**
     * 生成随机点
     *
     * @param seed  随机种子
     * @param range 坐标范围
     * @return 点的集合，每个点是 int[2]，表示 (x, y)
     */
    public static List<int[]> generatePoints(long seed, int range) {
        Random random = new Random(seed);
        List<int[]> points = new ArrayList<>();

        // 1. 随机生成点的数量 n = 1~3
        int n = random.nextInt(3) + 1;

        // 2. 生成圆心，x, y 在 1/3*range 到 2/3*range 之间
        int minCenter = range / 3;
        int maxCenter = 2 * range / 3;
        int centerX = minCenter + random.nextInt(maxCenter - minCenter + 1);
        int centerY = minCenter + random.nextInt(maxCenter - minCenter + 1);

        // 3. 随机生成半径 r = 1/6*range 到 1/4*range
        int minRadius = range / 6;
        int maxRadius = range / 4;
        double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);

        // 4. 在圆上生成 n 个点，每个点角度间隔随机 60° 到 360°/n
        double angle = 0;
        for (int i = 0; i < n; i++) {
            // 随机间隔角度
            double maxAngleStep = 360.0 / n;
            double angleStep = 60 + random.nextDouble() * (maxAngleStep - 60);
            angle += angleStep;

            // 转换为弧度
            double rad = Math.toRadians(angle);

            // 计算点坐标
            int x = centerX + (int) Math.round(radius * Math.cos(rad));
            int y = centerY + (int) Math.round(radius * Math.sin(rad));

            points.add(new int[]{x, y});
        }

        return points;
    }
}
