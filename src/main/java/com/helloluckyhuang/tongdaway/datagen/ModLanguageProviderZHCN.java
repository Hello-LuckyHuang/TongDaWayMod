package com.helloluckyhuang.tongdaway.datagen;

import com.helloluckyhuang.tongdaway.Tongdaway;
import com.helloluckyhuang.tongdaway.blocks.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModLanguageProviderZHCN extends LanguageProvider {
    public ModLanguageProviderZHCN(PackOutput output) {
        super(output, Tongdaway.MODID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        this.add(ModBlocks.TRACK_SPAWNER.get(), "轨道刷怪笼");
    }
}
