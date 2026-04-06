package com.helloluckyhuang.tongdaway.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(PackOutput output, Set<ResourceLocation> requiredTables, List<SubProviderEntry> subProviders) {
        super(output, requiredTables, subProviders);
    }
}
