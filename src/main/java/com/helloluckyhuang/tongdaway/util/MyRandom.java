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
    public static int[] generatePoints(long seed, int range) {
        Random random = new Random(seed);

        int quart = range / 4;

        // 先生成一个点
        int x1 = random.nextInt(quart, range - quart);
        int z1 = random.nextInt(quart, range - quart);

        return new int[]{x1, z1};
    }
}
