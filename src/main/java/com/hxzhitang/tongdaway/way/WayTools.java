package com.hxzhitang.tongdaway.way;

import com.hxzhitang.tongdaway.Config;
import com.hxzhitang.tongdaway.structure.GenerateStructure;
import com.hxzhitang.tongdaway.tools.TDWRandom;
import com.hxzhitang.tongdaway.util.blocks.SignNotesSetBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.Tags;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static com.hxzhitang.tongdaway.Tongdaway.CHUNK_GROUP_SIZE;

public class WayTools {
    final static BlockState landWayBlock = Blocks.DIRT_PATH.defaultBlockState();
    final static BlockState drtLandWayBlock = Blocks.SANDSTONE.defaultBlockState();
    final static BlockState coldLandWayBlock = Blocks.COBBLESTONE.defaultBlockState();
    final static BlockState wetWayBlock = Blocks.OAK_PLANKS.defaultBlockState();
    final static BlockState bridgeBlock = Blocks.OAK_PLANKS.defaultBlockState();

    final BlockState landWayFoundationBlock = Blocks.DIRT.defaultBlockState();

    public static void buildWaySign(WorldGenLevel worldGenLevel, RegionWayMap.WayPoint p, BlockPos realPos) {
        var biome = worldGenLevel.getBiome(realPos);
        boolean isWater = biome.is(Tags.Biomes.IS_WATER);

        if (!isWater) {
            GenerateStructure.generate(worldGenLevel, realPos, "way_signs/way_sign");
            BlockEntity blockEntity = worldGenLevel.getBlockEntity(realPos.offset(0, 4, 1));
            if (blockEntity instanceof SignNotesSetBlockEntity sign) {
                for (int z = -1; z <= 1; z+=2) {
                    sign.setScrollText(
                            "§dWay\n" + p.pointNote(),
                            new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, 1, z), true)
                    );
                    sign.setScrollText(
                            "§dWay\n" + getNoteFromConfig(3),
                            new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, 1, z), false)
                    );
                    sign.setActivation(true);
                    sign.setChanged();
                }
            }
        } else {
            GenerateStructure.generate(worldGenLevel, realPos.offset(0, -1, 0), "way_signs/way_sign_sea");
            BlockEntity blockEntity = worldGenLevel.getBlockEntity(realPos.offset(0, 0, 1));
            if (blockEntity instanceof SignNotesSetBlockEntity sign) {
                for (int z = -1; z <= 1; z+=2) {
                    sign.setScrollText(
                            "§dWay\n" + p.pointNote(),
                            new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, 0, z), true)
                    );
                }
                sign.setActivation(true);
                sign.setChanged();
            }
        }
    }

    public static void buildIntersection(WorldGenLevel worldGenLevel, RegionWayMap.WayPoint p, ChunkAccess chunkAccess, BlockPos realPos) {
        int x = realPos.getX();
        int z = realPos.getZ();
        var biome = worldGenLevel.getBiome(realPos);
        boolean isWater = biome.is(Tags.Biomes.IS_WATER);
        if (isWater) {
            GenerateStructure.generate(worldGenLevel, realPos.offset(-7, -1, -7), "intersection/sea_cross");
            BlockEntity blockEntity = worldGenLevel.getBlockEntity(realPos.offset(-1, 3, -1));
            if (blockEntity instanceof SignNotesSetBlockEntity sign) {
                sign.setScrollText(
                        p.pointNote(),
                        new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, -1, 1), true),
                        new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, -2, 1), true)
                );
                sign.setScrollText(
                        "§dAD Time\n" + getNoteFromConfig(7),
                        new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, -1, -1), true),
                        new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, -2, -1), true)
                );
                sign.setActivation(true);
                sign.setChanged();
            }
        } else {
            Random random = new Random(TDWRandom.getSeedByXZ(x, z));
            int r = random.nextInt(0, 3);
            if (r == 0) {
                GenerateStructure.generate(worldGenLevel, realPos.offset(-5, -1, -5), "intersection/small_cross0");
                fillFoundation(worldGenLevel, realPos.offset(-5, -1, -5), 9, 9);
                BlockEntity blockEntity = worldGenLevel.getBlockEntity(realPos.offset(-1, 0, -1));
                if (blockEntity instanceof SignNotesSetBlockEntity sign) {
                    sign.setScrollText(
                            p.pointNote(),
                            new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, 2, 1), true),
                            new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, 1, 1), true)
                    );
                    sign.setScrollText(
                            "§dAD Time\n" + getNoteFromConfig(7),
                            new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, 2, -1), true),
                            new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, 1, -1), true)
                    );
                    sign.setActivation(true);
                    sign.setChanged();
                }
            } else {
                GenerateStructure.generate(worldGenLevel, realPos.offset(-5, -1, -5), "intersection/large_cross0");
                fillFoundation(worldGenLevel, realPos.offset(-5, -1, -5), 12, 12);
                BlockEntity blockEntity = worldGenLevel.getBlockEntity(realPos.offset(-1, 0, -1));
                if (blockEntity instanceof SignNotesSetBlockEntity sign) {
                    sign.setScrollText(
                            p.pointNote(),
                            new SignNotesSetBlockEntity.SignMetaData(new BlockPos(1, 2, 0), true),
                            new SignNotesSetBlockEntity.SignMetaData(new BlockPos(1, 1, 0), true)
                    );
                    sign.setScrollText(
                            "§dAD Time\n" + getNoteFromConfig(7),
                            new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, 2, 1), true),
                            new SignNotesSetBlockEntity.SignMetaData(new BlockPos(0, 1, 1), true)
                    );
                    sign.setActivation(true);
                    sign.setChanged();
                }
            }
        }
    }

    public static void buildStreetlight(WorldGenLevel worldGenLevel, RegionWayMap.WayPoint p, ChunkAccess chunkAccess, BlockPos realPos) {
        int x = realPos.getX();
        int y = realPos.getY();
        int z = realPos.getZ();
        var biome = worldGenLevel.getBiome(realPos);
        boolean isWater = biome.is(Tags.Biomes.IS_WATER);
        if (isWater) {
            if (p.pointCode() % 2 == 1)
                GenerateStructure.generate(worldGenLevel, new BlockPos(x, y - 1, z), "streetlight/buoy0");
            return;
        }
        int px = (int) Math.floor(chunkAccess.getPos().x / (double) CHUNK_GROUP_SIZE);
        int pz = (int) Math.floor(chunkAccess.getPos().z / (double) CHUNK_GROUP_SIZE);
        Random random = new Random(TDWRandom.getSeedByXZ(px, pz));
        int r = random.nextInt(0, 3);
        GenerateStructure.generate(worldGenLevel, new BlockPos(x, y, z), "streetlight/streetlight" + r);
    }

    public static void buildWay(WorldGenLevel worldGenLevel, BlockPos realPos, int pointCode) {
        var biome = worldGenLevel.getBiome(realPos);
        //海上不生成路面
        if (biome.is(Tags.Biomes.IS_WATER) && (!biome.is(new ResourceLocation("minecraft:river"))))
            return;

        int dx = pointCode % 3;
        int dz = pointCode / 3;

        if (dx != 1) {
            // 修路
            for (int i = -1; i < 2; i++) {
                int x = realPos.getX();
                int z = realPos.getZ() + i;
                int h = realPos.getY();
                pavement(worldGenLevel, x, h, z, biome);
            }
        }
        if (dz != 1) {
            for (int j = -1; j < 2; j++) {
                int x = realPos.getX() + j;
                int z = realPos.getZ();
                int h = realPos.getY();
                pavement(worldGenLevel, x, h, z, biome);
            }
        }
    }

    //路基填充 桥架 隧道 上空障碍清除
    public static void wayFoundation(WorldGenLevel worldGenLevel, BlockPos realPos, int pointCode) {
        var biome = worldGenLevel.getBiome(realPos);
        //水上不生成路基
        boolean isWater = biome.is(Tags.Biomes.IS_WATER);
        if (isWater)
            return;

        int dx = pointCode % 3;
        int dz = pointCode / 3;

        int landH = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, realPos.getX(), realPos.getZ());

        if (realPos.getY() >= landH && realPos.getY() - landH <= 5) {
            // 高于地面
            for (int j = 0; j <= 3; j++) {
                for (int i = -2 - j; i < 3 + j; i++) {
                    int y = realPos.getY() - j;
                    if (dx != 1) {
                        int x = realPos.getX();
                        int z = realPos.getZ() + i;
                        int rh = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                        if (rh > y)
                            continue;
                        worldGenLevel.setBlock(new BlockPos(x, y, z), Blocks.GRASS_BLOCK.defaultBlockState(), 0);
                    }
                    if (dz != 1) {
                        int x = realPos.getX() + i;
                        int z = realPos.getZ();
                        int rh = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                        if (rh > y)
                            continue;
                        worldGenLevel.setBlock(new BlockPos(x, y, z), Blocks.GRASS_BLOCK.defaultBlockState(), 0);
                    }
                }
            }

            for (int i = -5; i < 6; i++) {
                int ry = realPos.getY() - 4;
                if (dx != 1) {
                    int x = realPos.getX();
                    int z = realPos.getZ() + i;
                    int rh = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                    if (rh >= ry)
                        continue;
                    for (int y = 0; y > rh - ry - 2; y--) {
                        worldGenLevel.setBlock(new BlockPos(x, ry + y, z), Blocks.DIRT.defaultBlockState(), 0);
                    }
                }
                if (dz != 1) {
                    int x = realPos.getX() + i;
                    int z = realPos.getZ();
                    int rh = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                    if (rh >= ry)
                        continue;
                    for (int y = 0; y > rh - ry - 2; y--) {
                        worldGenLevel.setBlock(new BlockPos(x, ry + y, z), Blocks.DIRT.defaultBlockState(), 0);
                    }
                }
            }
        } else if (realPos.getY() < landH && landH - realPos.getY() <= 5) {
            // 低于地面
            for (int j = 0; j <= 5; j++) {
                for (int i = -2 - j; i < 3 + j; i++) {
                    if (dx != 1) {
                        int x = realPos.getX();
                        int z = realPos.getZ() + i;
                        int y = realPos.getY() + j + 1;
                        worldGenLevel.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 0);
                    }
                    if (dz != 1) {
                        int x = realPos.getX() + i;
                        int z = realPos.getZ();
                        int y = realPos.getY() + j + 1;
                        worldGenLevel.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 0);
                    }
                }
                // 装饰路边 泥土替换草方块
                if (dx != 1) {
                    int x = realPos.getX();
                    int z = realPos.getZ() - 2 - j;
                    int y = realPos.getY() + j;
                    var bPos = new BlockPos(x, y, z);
                    if (worldGenLevel.getBlockState(bPos).equals(Blocks.DIRT.defaultBlockState())) {
                        worldGenLevel.setBlock(bPos, Blocks.GRASS_BLOCK.defaultBlockState(), 0);
                    }
                    z = realPos.getZ() + 2 + j;
                    bPos = new BlockPos(x, y, z);
                    if (worldGenLevel.getBlockState(bPos).equals(Blocks.DIRT.defaultBlockState())) {
                        worldGenLevel.setBlock(bPos, Blocks.GRASS_BLOCK.defaultBlockState(), 0);
                    }
                }
                if (dz != 1) {
                    int x = realPos.getX() - 2 - j;
                    int z = realPos.getZ();
                    int y = realPos.getY() + j;
                    var bPos = new BlockPos(x, y, z);
                    if (worldGenLevel.getBlockState(bPos).equals(Blocks.DIRT.defaultBlockState())) {
                        worldGenLevel.setBlock(bPos, Blocks.GRASS_BLOCK.defaultBlockState(), 0);
                    }
                    x = realPos.getX() + 2 + j;
                    bPos = new BlockPos(x, y, z);
                    if (worldGenLevel.getBlockState(bPos).equals(Blocks.DIRT.defaultBlockState())) {
                        worldGenLevel.setBlock(bPos, Blocks.GRASS_BLOCK.defaultBlockState(), 0);
                    }
                }

            }
        } else if (realPos.getY() >= landH && realPos.getY() - landH > 5) {
            // 架桥墩
            int x = realPos.getX();
            int z = realPos.getZ();
            if ((dx != 1 && x % 4 == 0) || (dz != 1 && z % 4 == 0) || (x % 4 == 0 && z % 4 == 0)) {
                for (int j = 0; j <= realPos.getY() - landH; j++) {
                    int y = realPos.getY() - j;
                    worldGenLevel.setBlock(new BlockPos(x, y, z), Blocks.DARK_OAK_FENCE.defaultBlockState(), 0);
                }
            }
        } else {
            // 隧道
            for (int i = 0; i < 5; i++) {
                for (int j = -2; j < 3; j++) {
                    if (dx != 1) {
                        int x = realPos.getX();
                        int z = realPos.getZ() + j;
                        int y = realPos.getY() + i + 1;
                        worldGenLevel.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 0);
                    }
                    if (dz != 1) {
                        int x = realPos.getX() + j;
                        int z = realPos.getZ();
                        int y = realPos.getY() + i + 1;
                        worldGenLevel.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 0);
                    }
                }
            }
        }
    }

    // 修路面
    private static void pavement(WorldGenLevel worldGenLevel, int x, int h, int z, Holder<Biome> biome) {
        BlockPos setPos = new BlockPos(x, h, z);
        var block = worldGenLevel.getBlockState(setPos.offset(0, -1, 0));
        if (!block.is(Blocks.WATER)) {
            boolean isDrt = biome.is(Tags.Biomes.IS_DRY); //.TAG.isDry(biome);
            boolean isCold = biome.is(Tags.Biomes.IS_COLD); //BiomeTag.TAG.isCold(biome);
            boolean isRiver = biome.is(new ResourceLocation("minecraft:river")); //BiomeTag.TAG.isRiver(biome);
            if (h - worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) > 5)
                worldGenLevel.setBlock(setPos, bridgeBlock, 0);
            else if (isCold)
                worldGenLevel.setBlock(setPos, coldLandWayBlock, 0);
            else if (isDrt)
                worldGenLevel.setBlock(setPos, drtLandWayBlock, 0);
            else if (isRiver) {
                worldGenLevel.setBlock(setPos, wetWayBlock, 0);
            } else
                worldGenLevel.setBlock(setPos, landWayBlock, 0);
//            for (int k = 1; k <= 3; k++)
//                worldGenLevel.setBlock(setPos.offset(0, k, 0), Blocks.AIR.defaultBlockState(), 0);
        } else
            worldGenLevel.setBlock(setPos, wetWayBlock, 0);
    }

    private static void fillFoundation(WorldGenLevel worldGenLevel, BlockPos realPos, int widthX, int widthZ) {
        int rx = realPos.getX();
        int ry = realPos.getY();
        int rz = realPos.getZ();
        for (int x = 0; x < widthX; x++) {
            for (int z = 0; z < widthZ; z++) {
                int landH = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, rx + x, rz + z);
                if (landH >= ry)
                    continue;
                for (int y = -1; y > landH - ry - 2; y--) {
                    worldGenLevel.setBlock(realPos.offset(x, y, z), Blocks.DIRT.defaultBlockState(), 0);
                }
            }
        }
    }

    private static String getNoteFromConfig(int n) {
        StringBuilder notes = new StringBuilder();

        if (Config.notes.isEmpty())
            return "";

        List<String> uniqueElements = new LinkedList<>(Config.notes);
        List<String> selectedElements = new LinkedList<>();
        Random random = new Random();

        while (selectedElements.size() < n && !uniqueElements.isEmpty()) {
            int r = random.nextInt(uniqueElements.size());
            String element = uniqueElements.get(r);
            selectedElements.add(element);
            uniqueElements.remove(element);
            notes.append(element).append("\n");
        }

        return notes.toString();
    }
}
