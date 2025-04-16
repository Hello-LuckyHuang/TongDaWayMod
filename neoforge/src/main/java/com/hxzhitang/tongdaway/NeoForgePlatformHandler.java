package com.hxzhitang.tongdaway;

import com.google.auto.service.AutoService;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

@AutoService(PlatformHandler.class)
public final class NeoForgePlatformHandler implements PlatformHandler {

    @Override
    public Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(Tongdaway.MODID);
    }

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tongdaway.MODID);

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(String key, Supplier<BlockEntityType.Builder<T>> builder) {
        return BLOCK_ENTITIES.register(key, () -> builder.get().build(Util.fetchChoiceType(References.BLOCK_ENTITY, key)));
    }

    private static final Map<ResourceKey<?>, DeferredRegister> CACHED = new Reference2ObjectOpenHashMap<>();

    @Override
    public <T> Supplier<T> register(Registry<? super T> registry, String name, Supplier<T> value) {
        return CACHED.computeIfAbsent(registry.key(), key -> DeferredRegister.create(registry.key().location(), Tongdaway.MODID)).register(name, value);
    }

    @Override
    public <T> Supplier<Holder.Reference<T>> registerForHolder(Registry<T> registry, String name, Supplier<T> value) {
        DeferredHolder<?, ?> registryObject = CACHED.computeIfAbsent(registry.key(), key -> DeferredRegister.create(registry.key().location(), Tongdaway.MODID)).register(name, value);
        return () -> (Holder.Reference<T>) registryObject.getDelegate();
    }

    public static void register(final IEventBus bus) {
        CACHED.values().forEach(deferredRegister -> deferredRegister.register(bus));
        BLOCK_ENTITIES.register(bus);
    }
}
