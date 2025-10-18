package com.hxzhitang.tongdaway.tools.graph;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Graph {
    List<Vertex> vertices;

    public Graph() {
        this.vertices = new ArrayList<>();
    }

    // ��Ӷ���
    public void addVertex(BlockPos vertexPos) {
        vertices.add(new Vertex(vertexPos));
    }

    // ��ӱ�
    public void addEdge(BlockPos from, BlockPos to) {
        Vertex fromVertex = getVertex(from);
        Vertex toVertex = getVertex(to);

        if (fromVertex != null && toVertex != null) {
            fromVertex.neighbors.add(toVertex);
            toVertex.neighbors.add(fromVertex); // ���������ͼ����Ҫ˫�����
        }
    }

    // ����ֵ��ȡ����
    private Vertex getVertex(BlockPos vertexPos) {
        for (Vertex vertex : vertices) {
            if (vertex.vertexPos == vertexPos) {
                return vertex;
            }
        }
        return null;
    }

    // ��ȡ����
    public List<Vertex> getVertexes() {
        return new ArrayList<>(vertices);
    }

    // ��������ӡÿ����
    public Set<Pair<Vertex, Vertex>> getEdges() {
        Set<String> visitedEdges = new HashSet<>(); // ���ڼ�¼�Ѿ����ʹ��ı�
        Set<Pair<Vertex, Vertex>> edges = new HashSet<>();

        for (Vertex vertex : vertices) {
            for (Vertex neighbor : vertex.neighbors) {
                // �����ظ���ӡͬһ����
                String edge = Math.min(vertex.vertexId, neighbor.vertexId) + "-" + Math.max(vertex.vertexId, neighbor.vertexId);
                if (!visitedEdges.contains(edge)) {
                    visitedEdges.add(edge);
                    edges.add(new Pair<>(vertex, neighbor));
                }
            }
        }

        return edges;
    }

    // ��ӡͼ
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