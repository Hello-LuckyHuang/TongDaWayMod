package com.hxzhitang.tongdaway.tools;

import java.util.*;

// 寻找最小成本路径（A*算法）
// 优化：使用八方向移动向量，预计算网格最小损耗（优化启发式函数）
// 参考：URL_ADDRESS// 参考：https://www.redblobgames.com/pathfinding/a-star/introduction.html
public class OptimizedAStarEightDirections {
    // 节点存储坐标和预估总成本f（g + h），用于优先队列排序
    static class Node implements Comparable<Node> {
        int x, y;
        double f; // f = g + h
        
        public Node(int x, int y, double f) {
            this.x = x;
            this.y = y;
            this.f = f;
        }
        
        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f, other.f);
        }
    }

    public static List<int[]> findMinimumCostPath(double[][] grid, int[] start, int[] end, AdditionalCostFunction additionalCostFunction) {
        // 此参数决定距离成本，若此成本较大，则更倾向走距离上更短的路。
        // 若此成本较小，则更倾向走损耗更小的路。
        final float distanceCost = 0.5f;

        int rows = grid.length;
        int cols = grid[0].length;
        
        // 预计算网格最小损耗（优化启发式函数）
        double minCost = Double.POSITIVE_INFINITY;
        for (double[] row : grid) {
            for (double val : row) {
                if (val < minCost) minCost = val;
            }
        }
        if (minCost == Double.POSITIVE_INFINITY) minCost = 1.0;

        // 八方向移动向量：上、下、左、右、左上、右上、左下、右下
        final int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};

        // 初始化g值（实际成本）和父节点
        double[][] g = new double[rows][cols];
        int[][][] parent = new int[rows][cols][2];
        for (int i = 0; i < rows; i++) {
            Arrays.fill(g[i], Double.POSITIVE_INFINITY);
        }
        g[start[0]][start[1]] = grid[start[0]][start[1]];
        parent[start[0]][start[1]] = new int[]{-1, -1};

        PriorityQueue<Node> openQueue = new PriorityQueue<>();
        double startH = heuristic(start[0], start[1], end[0], end[1], minCost);
        openQueue.add(new Node(start[0], start[1], g[start[0]][start[1]] + startH));

        while (!openQueue.isEmpty()) {
            Node current = openQueue.poll();
            int x = current.x, y = current.y;

            // 到达终点提前终止
            if (x == end[0] && y == end[1]) break;

            // 跳过非最优节点
            if (current.f > g[x][y] + heuristic(x, y, end[0], end[1], minCost)) continue;

            for (int[] dir : dirs) {
                int nx = x + dir[0], ny = y + dir[1];
                if (nx < 0 || nx >= rows || ny < 0 || ny >= cols) continue;

                // 计算移动成本
                boolean isDiagonal = (Math.abs(dir[0]) + Math.abs(dir[1])) == 2;
                double cost = grid[nx][ny] + distanceCost + (additionalCostFunction != null ? additionalCostFunction.cost(nx, ny) : 0);
                double stepCost = isDiagonal ? cost * Math.sqrt(2) : cost;
                double newG = g[x][y] + stepCost;

                // 更新更优路径
                if (newG < g[nx][ny]) {
                    g[nx][ny] = newG;
                    parent[nx][ny] = new int[]{x, y};
                    double h = heuristic(nx, ny, end[0], end[1], minCost);
                    openQueue.add(new Node(nx, ny, newG + h));
                }
            }
        }

        // 回溯路径
        List<int[]> path = new ArrayList<>();
        int x = end[0], y = end[1];
        if (g[x][y] == Double.POSITIVE_INFINITY) return path;

        while (x != -1 || y != -1) {
            path.add(new int[]{x, y});
            int[] p = parent[x][y];
            x = p[0];
            y = p[1];
        }
        Collections.reverse(path);
        return path;
    }

    // 启发式函数：欧几里得距离 × 网格最小损耗（保证可接受性）
    private static double heuristic(int x1, int y1, int x2, int y2, double minCost) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy) * minCost;
    }

    //附加损耗
    @FunctionalInterface
    public interface AdditionalCostFunction {
        double cost(int x, int y);
    }
}