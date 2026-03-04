package com.helloluckyhuang.tongdaway.datagen;

import com.helloluckyhuang.tongdaway.TongDaWay;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModDataGen extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, ModFeatures::bootstrap)
            .add(Registries.PLACED_FEATURE, ModPlacements::bootstrap)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap);

    public ModDataGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(TongDaWay.MODID));
    }

    /**
     * <p>模组数据生成方法
     * <p>不是，写NeoForge文档的人自己不看看自己写的是什么史吗？
     * <p>什么叫在静态方法里用this？为什么不把this改成event？
     * <p>主播现在很红
     * <p>代码，一定要可以编译！
     * <p>文档，一定要正确清晰！
     * <p>2025.12.30
     * <p><a href="https://docs.neoforged.net/docs/concepts/registries#data-generation-for-datapack-registries">事发地: NeoForge 文档</a>
     * <pre>{@code
     * @SubscribeEvent
     * public static void onGatherData(GatherDataEvent.Client event) {
     *     this.createDatapackRegistryObjects(
     *         new RegistrySetBuilder().add(...),
     *     );
     * }</pre>
     */
    public static void gatherData(GatherDataEvent.Client event) {
        // 特征、结构
        event.createDatapackRegistryObjects(BUILDER);

        // 语言文件
        event.createProvider(ModLanguageProviderENUS::new);
        event.createProvider(ModLanguageProviderZHCN::new);

        // 模型
        event.createProvider(ModBlockModelProvider::new);

        // 掉落表
        event.createProvider(pOutput -> new ModLootTableProvider(
                pOutput,
                Collections.emptySet(),
                List.of(
                        new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK)
                ),
                event.getLookupProvider()
        ));
    }
}
