package com.hxzhitang.tongdaway.structure;

import com.hxzhitang.tongdaway.Tongdaway;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class GenerateStructure {
    public static void generate(WorldGenLevel worldGenLevel, BlockPos pos, String structureId) {
        ServerLevel serverLevel = worldGenLevel.getLevel();
        StructureTemplateManager manager = serverLevel.getStructureManager();
        manager.get(ResourceLocation.fromNamespaceAndPath(Tongdaway.MODID, structureId)).ifPresent(template -> {
            // 生成结构
            template.placeInWorld(
                    worldGenLevel,
                    pos,
                    pos,
                    new StructurePlaceSettings()
                            .setRotation(Rotation.NONE)
                            .setMirror(Mirror.NONE)
                            .setIgnoreEntities(false),
                    serverLevel.random,
                    Block.UPDATE_CLIENTS
            );
        });
    }
}
