package com.hxzhitang.tongdaway.tools.graph;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.LinkedList;

public class Vertex {
    BlockPos vertexPos;
    int vertexId;
    LinkedList<Vertex> neighbors;

    public Vertex(BlockPos vertexPos) {
        this.vertexPos = vertexPos;
        this.neighbors = new LinkedList<>();
        this.vertexId = (vertexPos.getX() + "," + vertexPos.getZ()).hashCode();
    }

    public BlockPos getVertexPos() {
        return vertexPos;
    }
}
