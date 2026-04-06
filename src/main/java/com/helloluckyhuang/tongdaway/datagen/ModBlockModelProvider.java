package com.helloluckyhuang.tongdaway.datagen;

import com.helloluckyhuang.tongdaway.Tongdaway;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;


public class ModBlockModelProvider extends BlockModelProvider {
    public ModBlockModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Tongdaway.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        cubeAll("track_spawner",
                modLoc("block/track_spawner"));
    }
}
