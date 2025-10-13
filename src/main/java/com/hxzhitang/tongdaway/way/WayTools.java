package com.hxzhitang.tongdaway.way;

import com.hxzhitang.tongdaway.Config;
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
import net.minecraftforge.common.Tags;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static com.hxzhitang.tongdaway.Tongdaway.CHUNK_GROUP_SIZE;
import static com.hxzhitang.tongdaway.way.ChunkGroup.MAX_HEIGHT;

public class WayTools {
    final static BlockState landWayBlock = Blocks.DIRT_PATH.defaultBlockState();
    final static BlockState drtLandWayBlock = Blocks.SANDSTONE.defaultBlockState();
    final static BlockState coldLandWayBlock = Blocks.COBBLESTONE.defaultBlockState();
    final static BlockState wetWayBlock = Blocks.OAK_PLANKS.defaultBlockState();
    final static BlockState bridgeBlock = Blocks.OAK_PLANKS.defaultBlockState();
    final static BlockState undergroundBlock = Blocks.SMOOTH_STONE.defaultBlockState();

    final BlockState landWayFoundationBlock = Blocks.DIRT.defaultBlockState();

    public static void buildWaySign(WorldGenLevel worldGenLevel, RegionWayMap.WayPoint p, BlockPos realPos) {
        int landH = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, realPos.getX(), realPos.getZ());
        boolean isUnderground = landH - realPos.getY() > 5;
        if (isUnderground) {
            GenerateStructure.generate(worldGenLevel, realPos, "way_signs/underland_waysign");
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
            return;
        }

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
        int landH = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, realPos.getX(), realPos.getZ());
        boolean isUnderground = landH > MAX_HEIGHT-64;
        if (isUnderground) {
            var genPos = new BlockPos(realPos.getX(), MAX_HEIGHT-64-1, realPos.getZ());
            GenerateStructure.generate(worldGenLevel, genPos.offset(-5, -1, -5), "intersection/underland_cross");
            fillFoundation(worldGenLevel, genPos.offset(-5, -1, -5), 12, 12);
            BlockEntity blockEntity = worldGenLevel.getBlockEntity(genPos.offset(0, -1, 0));
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
            return;
        }

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
        int landH = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        boolean isUnderground = landH - y > 5;
        if (isUnderground) {
            GenerateStructure.generate(worldGenLevel, new BlockPos(x, y, z), "streetlight/underland_streetlight");
            return;
        }
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

        dx -= 1;
        dz -= 1;

        // 修路
        int x = realPos.getX();
        int z = realPos.getZ();
        int h = realPos.getY();

        int landH = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        boolean isUnderground = landH - h > 5;
        if (isUnderground) {
            pavementBlock(worldGenLevel, x, h, z, dx, dz, 1, undergroundBlock);
            return;
        }

        BlockPos setPos = new BlockPos(x, h, z);
        var block = worldGenLevel.getBlockState(setPos.offset(0, -1, 0));
        if (!block.is(Blocks.WATER)) {
            boolean isDrt = biome.is(Tags.Biomes.IS_DRY); //.TAG.isDry(biome);
            boolean isCold = biome.is(Tags.Biomes.IS_COLD); //BiomeTag.TAG.isCold(biome);
            boolean isRiver = biome.is(new ResourceLocation("minecraft:river"));
            if (h - worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) > 5) {
                pavementBlock(worldGenLevel, x, h, z, dx, dz, 2, bridgeBlock);
//                worldGenLevel.setBlock(new BlockPos(x + 2 * dz, h+1, z + 2 * (dx * (Math.abs(dz) - 1))), Blocks.DARK_OAK_FENCE.defaultBlockState(), 0);
//                worldGenLevel.setBlock(new BlockPos(x + 2 * (dz * (Math.abs(dx) - 1)), h+1, z + 2 * dx), Blocks.DARK_OAK_FENCE.defaultBlockState(), 0);
            }
//                worldGenLevel.setBlock(setPos, bridgeBlock, 0);
            else if (isCold)
                pavementBlock(worldGenLevel, x, h, z, dx, dz, 1, coldLandWayBlock);
//                worldGenLevel.setBlock(setPos, coldLandWayBlock, 0);
            else if (isDrt)
                pavementBlock(worldGenLevel, x, h, z, dx, dz, 1, drtLandWayBlock);
//                worldGenLevel.setBlock(setPos, drtLandWayBlock, 0);
            else if (isRiver) {
                pavementBlock(worldGenLevel, x, h, z, dx, dz, 2, wetWayBlock);
//                worldGenLevel.setBlock(setPos, wetWayBlock, 0);
            } else
                pavementBlock(worldGenLevel, x, h, z, dx, dz, 1, landWayBlock);
//                worldGenLevel.setBlock(setPos, landWayBlock, 0);
        } else
            pavementBlock(worldGenLevel, x, h, z, dx, dz, 1, wetWayBlock);
//            worldGenLevel.setBlock(setPos, wetWayBlock, 0);
    }

    //路基填充 桥架 隧道 上空障碍清除
    public static void wayFoundation(WorldGenLevel worldGenLevel, BlockPos realPos, int pointCode) {
        var biome = worldGenLevel.getBiome(realPos);
        //水上不生成路基
        boolean isWater = biome.is(new ResourceLocation("minecraft:river"));
        if (isWater)
            return;

        int dx = pointCode % 3;
        int dz = pointCode / 3;

        dx -= 1;
        dz -= 1;

        int landH = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, realPos.getX(), realPos.getZ());

        if (realPos.getY() >= landH && realPos.getY() - landH <= 5) {
            // 高于地面
            for (int j = 0; j <= 3; j++) {
                int y = realPos.getY() - j;
                int x = realPos.getX();
                int z = realPos.getZ();
                cheatAndPutBlock(worldGenLevel, x, y, z, Blocks.GRASS_BLOCK.defaultBlockState());
                for (int i = 1; i < 3 + j; i++) {
                    cheatAndPutBlock(worldGenLevel, x + i * dz, y, z + i * (dx * (Math.abs(dz) - 1)), Blocks.GRASS_BLOCK.defaultBlockState());
                    cheatAndPutBlock(worldGenLevel, x + i * (dz * (Math.abs(dx) - 1)), y, z + i * dx, Blocks.GRASS_BLOCK.defaultBlockState());
                }
            }

            for (int i = 1; i < 6; i++) {
                int ry = realPos.getY() - 4;
                int x = realPos.getX();
                int z = realPos.getZ();

                putBlockToLand(worldGenLevel, x + i * dz, ry, z + i * (dx * (Math.abs(dz) - 1)), Blocks.DIRT.defaultBlockState());
                putBlockToLand(worldGenLevel, x, ry, z, Blocks.DIRT.defaultBlockState());
                putBlockToLand(worldGenLevel, x + i * (dz * (Math.abs(dx) - 1)), ry, z + i * dx, Blocks.DIRT.defaultBlockState());
            }
        } else if (realPos.getY() < landH && landH - realPos.getY() <= 5) {
            // 低于地面
            // 需要白名单
            for (int j = 0; j <= 3; j++) {
                int x = realPos.getX();
                int z = realPos.getZ();
                int y = realPos.getY() + j + 1;
                worldGenLevel.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 0);
                for (int i = 1; i < 3 + j; i++) {
                    worldGenLevel.setBlock(new BlockPos(x + i * dz, y, z + i * (dx * (Math.abs(dz) - 1))), Blocks.AIR.defaultBlockState(), 0);
                    worldGenLevel.setBlock(new BlockPos(x + i * (dz * (Math.abs(dx) - 1)), y, z + i * dx), Blocks.AIR.defaultBlockState(), 0);
                }
                // 装饰路边 泥土替换草方块
                int x2 = realPos.getX();
                int z2 = realPos.getZ();
                int y2 = realPos.getY() + j;
                var bPos = new BlockPos(x2 + (j+2) * dz, y2, z2 + (j+2) * (dx * (Math.abs(dz) - 1)));
                if (worldGenLevel.getBlockState(bPos).equals(Blocks.DIRT.defaultBlockState())
                || worldGenLevel.getBlockState(bPos).equals(Blocks.STONE.defaultBlockState())) {
                    worldGenLevel.setBlock(bPos, Blocks.GRASS_BLOCK.defaultBlockState(), 0);
                }
                bPos = new BlockPos(x2 + (j+2) * (dz * (Math.abs(dx) - 1)), y2, z2 + (j+2) * dx);
                if (worldGenLevel.getBlockState(bPos).equals(Blocks.DIRT.defaultBlockState())
                || worldGenLevel.getBlockState(bPos).equals(Blocks.STONE.defaultBlockState())) {
                    worldGenLevel.setBlock(bPos, Blocks.GRASS_BLOCK.defaultBlockState(), 0);
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
                int x = realPos.getX();
                int z = realPos.getZ();
                int y = realPos.getY() + i + 1;
                worldGenLevel.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 0);
                for (int j = 1; j < 3; j++) {
                    worldGenLevel.setBlock(new BlockPos(x + j * dz, y, z + j * (dx * (Math.abs(dz) - 1))), Blocks.AIR.defaultBlockState(), 0);
                    worldGenLevel.setBlock(new BlockPos(x + j * (dz * (Math.abs(dx) - 1)), y, z + j * dx), Blocks.AIR.defaultBlockState(), 0);
                }
            }
        }
    }

    public static void buildPier(WorldGenLevel worldGenLevel, BlockPos realPos) {
        var biome = worldGenLevel.getBiome(realPos);
        //水上不生成路基
        boolean isWater = biome.is(new ResourceLocation("minecraft:river"));
        if (isWater)
            return;

        int landH = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, realPos.getX(), realPos.getZ());

        if (realPos.getY() >= landH && realPos.getY() - landH > 5) {
            // 架桥墩
            int x = realPos.getX();
            int y = realPos.getY() - 1;
            int z = realPos.getZ();
            putBlockToLand(worldGenLevel, x, y, z, Blocks.STONE_BRICKS.defaultBlockState());
        }
    }

    // 修路面
    private static void pavementBlock(WorldGenLevel worldGenLevel, int x, int h, int z, int ddx, int ddz, int width, BlockState blockState) {
        worldGenLevel.setBlock(new BlockPos(x, h, z), blockState, 0);
        for (int i = 1; i <= width; i++) {
            worldGenLevel.setBlock(new BlockPos(x + i * ddz, h, z + i * (ddx * (Math.abs(ddz) - 1))), blockState, 0);
            worldGenLevel.setBlock(new BlockPos(x + i * (ddz * (Math.abs(ddx) - 1)), h, z + i * ddx), blockState, 0);
        }
    }

    private static void cheatAndPutBlock(WorldGenLevel worldGenLevel, int x, int y, int z, BlockState blockState) {
        int rh = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        if (rh > y)
            return;
        worldGenLevel.setBlock(new BlockPos(x, y, z), blockState, 0);
    }

    // 从ry高度开始，向陆地上放置方块
    private static void putBlockToLand(WorldGenLevel worldGenLevel, int x, int ry, int z, BlockState blockState) {
        int rh = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        if (rh >= ry)
            return;
        for (int y = 0; y > rh - ry - 2; y--) {
            worldGenLevel.setBlock(new BlockPos(x, ry + y, z), Blocks.DIRT.defaultBlockState(), 0);
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
