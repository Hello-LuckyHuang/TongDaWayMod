package com.hxzhitang.tongdaway.tools.graph;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Graph {
    List<Vertex> vertices;

    public Graph() {
        this.vertices = new ArrayList<>();
    }

    // 添加顶点
    public void addVertex(BlockPos vertexPos) {
        vertices.add(new Vertex(vertexPos));
    }

    // 添加边
    public void addEdge(BlockPos from, BlockPos to) {
        Vertex fromVertex = getVertex(from);
        Vertex toVertex = getVertex(to);

        if (fromVertex != null && toVertex != null) {
            fromVertex.neighbors.add(toVertex);
            toVertex.neighbors.add(fromVertex); // 如果是无向图，需要双向添加
        }
    }

    // 根据值获取顶点
    private Vertex getVertex(BlockPos vertexPos) {
        for (Vertex vertex : vertices) {
            if (vertex.vertexPos == vertexPos) {
                return vertex;
            }
        }
        return null;
    }

    // 获取顶点
    public List<Vertex> getVertexes() {
        return new ArrayList<>(vertices);
    }

    // 遍历并打印每条边
    public Set<Pair<Vertex, Vertex>> getEdges() {
        Set<String> visitedEdges = new HashSet<>(); // 用于记录已经访问过的边
        Set<Pair<Vertex, Vertex>> edges = new HashSet<>();

        for (Vertex vertex : vertices) {
            for (Vertex neighbor : vertex.neighbors) {
                // 避免重复打印同一条边
                String edge = Math.min(vertex.vertexId, neighbor.vertexId) + "-" + Math.max(vertex.vertexId, neighbor.vertexId);
                if (!visitedEdges.contains(edge)) {
                    visitedEdges.add(edge);
                    edges.add(new Pair<>(vertex, neighbor));
                }
            }
        }

        return edges;
    }

    // 打印图
    public void printGraph() {
        for (Vertex vertex : vertices) {
            System.out.print("Vertex " + vertex.vertexPos + " is connected to: ");
            for (Vertex neighbor : vertex.neighbors) {
                System.out.print(neighbor.vertexPos + " ");
            }
            System.out.println();
        }
    }
}