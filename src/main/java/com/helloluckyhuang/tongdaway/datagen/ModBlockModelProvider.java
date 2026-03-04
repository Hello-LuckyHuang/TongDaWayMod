package com.helloluckyhuang.tongdaway.datagen;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.blocks.ModBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;


public class ModBlockModelProvider extends ModelProvider {
    public ModBlockModelProvider(PackOutput output) {
        super(output, TongDaWay.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Block track = ModBlocks.TRACK_SPAWNER.get();

        blockModels.createTrivialCube(track);
    }
}
