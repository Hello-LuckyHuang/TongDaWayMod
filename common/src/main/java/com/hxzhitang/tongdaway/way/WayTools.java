package com.hxzhitang.tongdaway.way;

import com.hxzhitang.tongdaway.BiomeTag;
import com.hxzhitang.tongdaway.ConfigVar;
import com.hxzhitang.tongdaway.structure.GenerateStructure;
import com.hxzhitang.tongdaway.tools.TDWRandom;
import com.hxzhitang.tongdaway.util.blocks.SignNotesSetBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

import static com.hxzhitang.tongdaway.Common.CHUNK_GROUP_SIZE;

public class WayTools {
    public static void buildWaySign(WorldGenLevel worldGenLevel, RegionWayMap.WayPoint p, BlockPos realPos) {
        var biome = worldGenLevel.getBiome(realPos);
        boolean isWater = BiomeTag.TAG.isDry(biome);

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
            GenerateStructure.generate(worldGenLevel, realPos.offset(0, -1,0), "way_signs/way_sign_sea");
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
        boolean isWater = BiomeTag.TAG.isWet(biome);
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
        boolean isWater = BiomeTag.TAG.isWet(biome);
        if (isWater) {
            if (p.pointId() % 2 == 1)
                GenerateStructure.generate(worldGenLevel, new BlockPos(x, y - 1, z), "streetlight/buoy0");
            return;
        }
        int px = (int) Math.floor(chunkAccess.getPos().x / (double) CHUNK_GROUP_SIZE);
        int pz = (int) Math.floor(chunkAccess.getPos().z / (double) CHUNK_GROUP_SIZE);
        Random random = new Random(TDWRandom.getSeedByXZ(px, pz));
        int r = random.nextInt(0, 3);
        GenerateStructure.generate(worldGenLevel, new BlockPos(x, y, z), "streetlight/streetlight" + r);
    }

    public static void buildWay(WorldGenLevel worldGenLevel, ChunkAccess chunkAccess, BlockPos realPos, String wayType) {
        final BlockState landWayBlock = Blocks.DIRT_PATH.defaultBlockState();
        final BlockState drtLandWayBlock = Blocks.SANDSTONE.defaultBlockState();
        final BlockState coldLandWayBlock = Blocks.COBBLESTONE.defaultBlockState();
        final BlockState wetWayBlock = Blocks.OAK_PLANKS.defaultBlockState();

        var biome = worldGenLevel.getBiome(realPos);
        boolean isWater = BiomeTag.TAG.isWet(biome);
        if (isWater) {
            if (!biome.is(new ResourceLocation("minecraft:river")))
                return;
        }

        if (wayType.equals("way")) {
            int[][] dirs = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
            for (int[] dir : dirs) {
                int x = realPos.getX() + dir[0];
                int z = realPos.getZ() + dir[1];
                int h = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;

                BlockPos setPos = new BlockPos(x, h, z);

                var block = worldGenLevel.getBlockState(setPos.offset(0, -1, 0));
                if (!block.is(Blocks.WATER)) {
                    boolean isDrt = BiomeTag.TAG.isDry(biome);
                    boolean isCold = BiomeTag.TAG.isCold(biome);
                    if (isCold)
                        worldGenLevel.setBlock(setPos, coldLandWayBlock, 0);
                    else if (isDrt)
                        worldGenLevel.setBlock(setPos, drtLandWayBlock, 0);
                    else
                        worldGenLevel.setBlock(setPos, landWayBlock, 0);
                } else
                    worldGenLevel.setBlock(setPos, wetWayBlock, 0);
            }
        } else if (wayType.equals("bridge")) {
            int cy = realPos.getY();
            for (int ix = -1; ix <= 1; ix++) {
                for (int iz = -1; iz <= 1; iz++) {
                    int x = realPos.getX() + ix;
                    int z = realPos.getZ() + iz;
                    BlockPos setPos = new BlockPos(x, cy, z);
                    worldGenLevel.setBlock(setPos, wetWayBlock, 0);
                }
            }
        }

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

        if (ConfigVar.notes.isEmpty())
            return "";

        List<String> uniqueElements = new LinkedList<>(ConfigVar.notes);
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
