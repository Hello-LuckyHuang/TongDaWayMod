package com.hxzhitang.tongdaway.tools;

public class ExpandImage {
    public static double[][] expandImage(int[][] originalImage, int n) {
        int originalHeight = originalImage.length;
        int originalWidth = originalImage[0].length;
        int newHeight = originalHeight * n;
        int newWidth = originalWidth * n;

        double[][] expandedImage = new double[newHeight][newWidth];

        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                double originalX = (double) x / n;
                double originalY = (double) y / n;

                int x1 = (int) Math.floor(originalX);
                int x2 = Math.min(x1 + 1, originalWidth - 1);
                int y1 = (int) Math.floor(originalY);
                int y2 = Math.min(y1 + 1, originalHeight - 1);

                double dx = originalX - x1;
                double dy = originalY - y1;

                double value1 = originalImage[y1][x1] * (1 - dx) * (1 - dy);
                double value2 = originalImage[y1][x2] * dx * (1 - dy);
                double value3 = originalImage[y2][x1] * (1 - dx) * dy;
                double value4 = originalImage[y2][x2] * dx * dy;

                expandedImage[y][x] = value1 + value2 + value3 + value4;
            }
        }

        return expandedImage;
    }
}

