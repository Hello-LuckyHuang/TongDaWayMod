package com.hxzhitang.tongdaway.tools;

import java.util.*;

// Ѱ����С�ɱ�·����A*�㷨��
// �Ż���ʹ�ð˷����ƶ�������Ԥ����������С��ģ��Ż�����ʽ������
// �ο���URL_ADDRESS// �ο���https://www.redblobgames.com/pathfinding/a-star/introduction.html
public class OptimizedAStarEightDirections {
    // �ڵ�洢�����Ԥ���ܳɱ�f��g + h�����������ȶ�������
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
        // �˲�����������ɱ������˳ɱ��ϴ���������߾����ϸ��̵�·��
        // ���˳ɱ���С�������������ĸ�С��·��
        final float distanceCost = 0.5f;

        int rows = grid.length;
        int cols = grid[0].length;
        
        // Ԥ����������С��ģ��Ż�����ʽ������
        double minCost = Double.POSITIVE_INFINITY;
        for (double[] row : grid) {
            for (double val : row) {
                if (val < minCost) minCost = val;
            }
        }
        if (minCost == Double.POSITIVE_INFINITY) minCost = 1.0;

        // �˷����ƶ��������ϡ��¡����ҡ����ϡ����ϡ����¡�����
        final int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};

        // ��ʼ��gֵ��ʵ�ʳɱ����͸��ڵ�
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

            // �����յ���ǰ��ֹ
            if (x == end[0] && y == end[1]) break;

            // ���������Žڵ�
            if (current.f > g[x][y] + heuristic(x, y, end[0], end[1], minCost)) continue;

            for (int[] dir : dirs) {
                int nx = x + dir[0], ny = y + dir[1];
                if (nx < 0 || nx >= rows || ny < 0 || ny >= cols) continue;

                // �����ƶ��ɱ�
                boolean isDiagonal = (Math.abs(dir[0]) + Math.abs(dir[1])) == 2;
                double cost = grid[nx][ny] + distanceCost + (additionalCostFunction != null ? additionalCostFunction.cost(nx, ny) : 0);
                double stepCost = isDiagonal ? cost * Math.sqrt(2) : cost;
                double newG = g[x][y] + stepCost;

                // ���¸���·��
                if (newG < g[nx][ny]) {
                    g[nx][ny] = newG;
                    parent[nx][ny] = new int[]{x, y};
                    double h = heuristic(nx, ny, end[0], end[1], minCost);
                    openQueue.add(new Node(nx, ny, newG + h));
                }
            }
        }

        // ����·��
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

    // ����ʽ������ŷ����þ��� �� ������С��ģ���֤�ɽ����ԣ�
    private static double heuristic(int x1, int y1, int x2, int y2, double minCost) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy) * minCost;
    }

    //�������
    @FunctionalInterface
    public interface AdditionalCostFunction {
        double cost(int x, int y);
    }
}