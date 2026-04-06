package com.helloluckyhuang.tongdaway.datagen;

import com.helloluckyhuang.tongdaway.Tongdaway;
import com.helloluckyhuang.tongdaway.blocks.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModLanguageProviderENUS extends LanguageProvider {
    public ModLanguageProviderENUS(PackOutput output) {
        super(output, Tongdaway.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        this.add(ModBlocks.TRACK_SPAWNER.get(), "Track Spawner");
    }
}
