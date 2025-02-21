package com.hxzhitang.tongdaway.way;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

@FunctionalInterface
public interface CreateStructuresFunction {
    void createStructures(RegistryAccess p_255835_, ChunkGeneratorStructureState p_256505_, StructureManager p_255934_, ChunkAccess p_255767_, StructureTemplateManager p_255832_);
}
