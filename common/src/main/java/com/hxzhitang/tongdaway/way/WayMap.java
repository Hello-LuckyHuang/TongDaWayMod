package com.hxzhitang.tongdaway.way;

import com.hxzhitang.tongdaway.ConfigVar;
import com.hxzhitang.tongdaway.tools.TDWRandom;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import java.util.*;

import static com.hxzhitang.tongdaway.Common.CHUNK_GROUP_SIZE;


public class WayMap {
    private final int x;
    private final int z;
    private final long seed;

    private final Map<Pair<BlockPos, BlockPos>, String> ways = new HashMap<>();
    private BlockPos[] nodes = null;
    private final Map<BlockPos, String> intersection = new HashMap<>();
    private final Map<BlockPos, String> linkedStructure = new HashMap<>();

    private int[][] wayImg = null;

    public WayMap(int x, int z, long seed) {
        this.x = x;
        this.z = z;
        this.seed = seed;
        spawnGraph();
    }

    public Map<Pair<BlockPos, BlockPos>, String> getWays() {
        return ways;
    }

    /*
    ·�����ɣ�ÿ��������(16*16)����1-2���ڵ㣬����������������Ľڵ����ӡ�
    �����һ���ڵ㣬��������ÿһ���ڵ�����
    ����������ڵ㣬��������������еĽڵ�����
    Ȼ���ڴ�ׯ������Ϻ󣬴ӽڵ����Ӵ�ׯ
     */

    /**
     * ��ÿ��������(16*16 ����)�������е���ͨ·���ڵ�
     * ��ʱ����
     * @param x ������x����
     * @param z ������z����
     * @return ���ɵ�·�������飬1-2��
     */
    public BlockPos[] spawnWayNode(long seed, int x, int z) {
        Random random = new Random(TDWRandom.getSeedByXZ(seed, x, z));
        int nodeNum = random.nextInt(1, 3);
        BlockPos[] nodes = new BlockPos[nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            random.setSeed(TDWRandom.getSeedByXZ(seed, x, z * 10 + i));
            int nx = random.nextInt(2, CHUNK_GROUP_SIZE - 2) + x * CHUNK_GROUP_SIZE;
            random.setSeed(TDWRandom.getSeedByXZ(seed, x * 10 + i, z));
            int nz = random.nextInt(2, CHUNK_GROUP_SIZE - 2) + z * CHUNK_GROUP_SIZE;
            nodes[i] = new BlockPos(nx * 16 + 8, 0, nz * 16 + 8);
        }

        return nodes;
    }

    /**
     * ����·��ͼ��������ͨ�ڵ��
     * ��ʱ����
     */
    private void spawnGraph() {
        //�����Լ��ĵ����Χ�ĸ�·��ͼ�ĵ�
        BlockPos[] selfNodes = spawnWayNode(seed, x, z);
        List<BlockPos[]> otherNodes = new ArrayList<>();
        for (BlockPos pos : selfNodes) {
            intersection.put(pos, "��dCross");
        }
        for (int px = -1; px < 2; px += 2) {
            BlockPos[] otherNodeX = spawnWayNode(seed, x+px, z);
            int finalPx = px;
            //����ʹ�����鿪ͷ�����������
            Arrays.sort(otherNodeX, Comparator.comparingInt(n -> finalPx * n.getX()));
            otherNodes.add(otherNodeX);
        }
        for (int pz = -1; pz < 2; pz += 2) {
            BlockPos[] otherNodeZ = spawnWayNode(seed, x, z+pz);
            int finalPz = pz;
            //����ʹ�����鿪ͷ�����������
            Arrays.sort(otherNodeZ, Comparator.comparingInt(n -> finalPz * n.getZ()));
            otherNodes.add(otherNodeZ);
        }

        //Ȼ����ݹ������ɱ�
        Random random = new Random();
        if (selfNodes.length == 1) {
            //�����·��ͼ��ֻ��һ���ڵ�
            //������������·��ͼ�Ľڵ�
            random.setSeed(TDWRandom.getSeedByXZ(seed, x, 0));
            int[] z0s = {selfNodes[0].getZ(), otherNodes.get(0)[0].getZ()};
            Arrays.sort(z0s);
            int z0 = random.nextInt(z0s[0], z0s[1] + 1);
            var edgeNode0 = new BlockPos(this.x * CHUNK_GROUP_SIZE * 16, 0, z0);
            ways.put(Pair.of(selfNodes[0], edgeNode0), "West");
            intersection.put(selfNodes[0], intersection.get(selfNodes[0])+"\nWest");

            random.setSeed(TDWRandom.getSeedByXZ(seed, x + 1,  0));
            int[] z1s = {selfNodes[0].getZ(), otherNodes.get(1)[0].getZ()};
            Arrays.sort(z1s);
            int z1 = random.nextInt(z1s[0], z1s[1] + 1);
            var edgeNode1 = new BlockPos((this.x + 1) * CHUNK_GROUP_SIZE * 16 - 1, 0, z1);
            ways.put(Pair.of(selfNodes[0], edgeNode1), "East");
            intersection.put(selfNodes[0], intersection.get(selfNodes[0])+"\nEast");

            random.setSeed(TDWRandom.getSeedByXZ(seed, 0, z));
            int[] x0s = {selfNodes[0].getX(), otherNodes.get(2)[0].getX()};
            Arrays.sort(x0s);
            int x0 = random.nextInt(x0s[0], x0s[1] + 1);
            var edgeNode2 = new BlockPos(x0, 0, this.z * CHUNK_GROUP_SIZE * 16);
            ways.put(Pair.of(selfNodes[0], edgeNode2), "South");
            intersection.put(selfNodes[0], intersection.get(selfNodes[0])+"\nSouth");

            random.setSeed(TDWRandom.getSeedByXZ(seed, 0, z + 1));
            int[] x1s = {selfNodes[0].getX(), otherNodes.get(3)[0].getX()};
            Arrays.sort(x1s);
            int x1 = random.nextInt(x1s[0], x1s[1] + 1);
            var edgeNode3 = new BlockPos(x1, 0, (this.z + 1) * CHUNK_GROUP_SIZE * 16 - 1);
            ways.put(Pair.of(selfNodes[0], edgeNode3), "North");
            intersection.put(selfNodes[0], intersection.get(selfNodes[0])+"\nNorth");
        } else {
            //���������
            //�Ȱ�x���򣬴������С������
            Arrays.sort(selfNodes, Comparator.comparingInt(Vec3i::getX));
            random.setSeed(TDWRandom.getSeedByXZ(seed, x, 0));
            int[] z0s = {selfNodes[0].getZ(), otherNodes.get(0)[0].getZ()};
            Arrays.sort(z0s);
            int z0 = random.nextInt(z0s[0], z0s[1] + 1);
            var edgeNode0 = new BlockPos(this.x * CHUNK_GROUP_SIZE * 16, 0, z0);
            ways.put(Pair.of(selfNodes[0], edgeNode0), "West");
            intersection.put(selfNodes[0], intersection.get(selfNodes[0])+"\nWest");

            random.setSeed(TDWRandom.getSeedByXZ(seed, x + 1, 0));
            int[] z1s = {selfNodes[1].getZ(), otherNodes.get(1)[0].getZ()};
            Arrays.sort(z1s);
            int z1 = random.nextInt(z1s[0], z1s[1] + 1);
            var edgeNode1 = new BlockPos((this.x + 1) * CHUNK_GROUP_SIZE * 16 - 1, 0, z1);
            ways.put(Pair.of(selfNodes[1], edgeNode1), "East");
            intersection.put(selfNodes[1], intersection.get(selfNodes[1])+"\nEast");

            //Ȼ��z����
            //�������������ڵ㣬�������С������
            Arrays.sort(selfNodes, Comparator.comparingInt(Vec3i::getZ));
            random.setSeed(TDWRandom.getSeedByXZ(seed, 0, z));
            int[] x0s = {selfNodes[0].getX(), otherNodes.get(2)[0].getX()};
            Arrays.sort(x0s);
            int x0 = random.nextInt(x0s[0], x0s[1] + 1);
            var edgeNode2 = new BlockPos(x0, 0, this.z * CHUNK_GROUP_SIZE * 16);
            ways.put(Pair.of(selfNodes[0], edgeNode2), "South");
            intersection.put(selfNodes[0], intersection.get(selfNodes[0])+"\nSouth");

            random.setSeed(TDWRandom.getSeedByXZ(seed, 0, z + 1));
            int[] x1s = {selfNodes[1].getX(), otherNodes.get(3)[0].getX()};
            Arrays.sort(x1s);
            int x1 = random.nextInt(x1s[0], x1s[1] + 1);
            var edgeNode3 = new BlockPos(x1, 0, (this.z + 1) * CHUNK_GROUP_SIZE * 16 - 1);
            ways.put(Pair.of(selfNodes[1], edgeNode3), "North");
            intersection.put(selfNodes[1], intersection.get(selfNodes[1])+"\nNorth");

            //��������Լ�������
            ways.put(Pair.of(selfNodes[0], selfNodes[1]), "Next Cross");
            intersection.put(selfNodes[0], intersection.get(selfNodes[0])+"\nNext Cross");
            intersection.put(selfNodes[1], intersection.get(selfNodes[1])+"\nNext Cross");
        }
        nodes = selfNodes;
    }

    public Map<BlockPos, String> getIntersection() {
        return intersection;
    }

    public int getWayValue(int cx, int cz, int bx, int bz) {
        if (wayImg == null)
            return 0;
        int px = cx % CHUNK_GROUP_SIZE;
        int pz = cz % CHUNK_GROUP_SIZE;
        px = px >= 0 ? px : CHUNK_GROUP_SIZE + px;
        pz = pz >= 0 ? pz : CHUNK_GROUP_SIZE + pz;
        return wayImg[px*16+bx][pz*16+bz];
    }

    public void setStructureNode(BlockPos structureBlockPos, String structureName) {
        //�����ظ�
        linkedStructure.put(structureBlockPos, structureName);
    }

    //���������ڽṹ����������
    public void applyStructureNode() {
        //ѡ�񼸸��ṹ��
        Random random = new Random(TDWRandom.getSeedByXZ(seed, x, z));
        for (int i = linkedStructure.size(); i > ConfigVar.connectFeaturesNum; i--) {
            int r = random.nextInt(0, linkedStructure.size());
            BlockPos key = (BlockPos) linkedStructure.keySet().toArray()[r];
            linkedStructure.remove(key);
        }
        //���������·��
        BlockPos nearestNode = null;
        for (BlockPos structureBlockPos : linkedStructure.keySet()) {
            String name = linkedStructure.get(structureBlockPos);
            for (BlockPos node : intersection.keySet()) {
                double distance = Math.sqrt(Math.pow(node.getX() - structureBlockPos.getX(), 2) + Math.pow(node.getZ() - structureBlockPos.getZ(), 2));
                if (nearestNode == null || distance < Math.sqrt(Math.pow(nearestNode.getX() - structureBlockPos.getX(), 2) + Math.pow(nearestNode.getZ() - structureBlockPos.getZ(), 2))) {
                    nearestNode = node;
                }
            }

            if (nearestNode != null) {
                String wayName = name;
                if (name.contains(":"))
                    wayName = name.split(":")[1];
                wayName = wayName.replace("_", " ");
                ways.put(Pair.of(nearestNode, structureBlockPos), wayName);
                intersection.put(nearestNode, intersection.get(nearestNode)+"\n"+wayName);
            }
        }
    }
}
