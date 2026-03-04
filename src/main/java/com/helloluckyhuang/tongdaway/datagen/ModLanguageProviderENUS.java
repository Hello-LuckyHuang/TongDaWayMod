package com.helloluckyhuang.tongdaway.datagen;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.blocks.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProviderENUS extends LanguageProvider {
    public ModLanguageProviderENUS(PackOutput output) {
        super(output, TongDaWay.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        this.add(ModBlocks.TRACK_SPAWNER.get(), "Track Spawner");
    }
}
