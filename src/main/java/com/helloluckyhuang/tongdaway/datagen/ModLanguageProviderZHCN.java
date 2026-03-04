package com.helloluckyhuang.tongdaway.datagen;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.blocks.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProviderZHCN extends LanguageProvider {
    public ModLanguageProviderZHCN(PackOutput output) {
        super(output, TongDaWay.MODID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        this.add(ModBlocks.TRACK_SPAWNER.get(), "轨道刷怪笼");
    }
}
