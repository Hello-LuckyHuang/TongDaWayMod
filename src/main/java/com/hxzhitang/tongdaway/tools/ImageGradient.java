package com.hxzhitang.tongdaway.tools;

import java.util.Arrays;

//计算图像梯度
public class ImageGradient {
	public static double[][] calculateGradient(double[][] image) {
        int width = image.length;
        int height = image[0].length;

        double[][] gradient = new double[width][height];
        
        Arrays.fill(gradient[0], Double.POSITIVE_INFINITY);
        Arrays.fill(gradient[width - 1], Double.POSITIVE_INFINITY);
        for (int i = 1; i < width - 1; i++) {
        	gradient[i][0] = Double.POSITIVE_INFINITY;
        	gradient[i][height - 1] = Double.POSITIVE_INFINITY;
		}

        // Sobel算子
        int[][] sobelX = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
        };

        int[][] sobelY = {
            {1, 2, 1},
            {0, 0, 0},
            {-1, -2, -1}
        };

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double gx = 0;
                double gy = 0;

                for (int j = -1; j <= 1; j++) {
                    for (int i = -1; i <= 1; i++) {
                        gx += image[x + i][y + j] * sobelX[i + 1][j + 1];
                        gy += image[x + i][y + j] * sobelY[i + 1][j + 1];
                    }
                }

                gradient[x][y] = Math.sqrt(gx * gx + gy * gy);
            }
        }
        //复制梯度到边缘
        for (int y = 1; y < height - 1; y++) {
            gradient[0][y] = gradient[1][y];
            gradient[width - 1][y] = gradient[width - 2][y];
        }
        for (int x = 1; x < width - 1; x++) {
            gradient[x][0] = gradient[x][1];
            gradient[x][height - 1] = gradient[x][height - 2];
        }
        // 处理四角
        gradient[0][0] = gradient[1][1];
        gradient[0][height - 1] = gradient[1][height - 2];
        gradient[width - 1][0] = gradient[width - 2][1];
        gradient[width - 1][height - 1] = gradient[width - 2][height - 2];

        return gradient;
    }
}
