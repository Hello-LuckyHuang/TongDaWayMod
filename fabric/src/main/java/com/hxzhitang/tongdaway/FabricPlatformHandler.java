package com.hxzhitang.tongdaway;

import com.google.auto.service.AutoService;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.nio.file.Path;
import java.util.function.Supplier;

@AutoService(PlatformHandler.class)
public final class FabricPlatformHandler implements PlatformHandler {

    @Override
    public Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(TongDaWay.MODID);
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(String key, Supplier<BlockEntityType.Builder<T>> builder) {
        ResourceLocation resourceLocation = Common.id(key);
        BlockEntityType<T> blockEntity = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, resourceLocation, builder.get().build(Util.fetchChoiceType(References.BLOCK_ENTITY, resourceLocation.toString())));
        return () -> blockEntity;
    }

    @Override
    public <T> Supplier<T> register(Registry<? super T> registry, String name, Supplier<T> value) {
        T value1 = Registry.register(registry, Common.id(name), value.get());
        return () -> value1;
    }

    @Override
    public <T> Supplier<Holder.Reference<T>> registerForHolder(Registry<T> registry, String name, Supplier<T> value) {
        Holder.Reference<T> reference = Registry.registerForHolder(registry, Common.id(name), value.get());
        return () -> reference;
    }
}
