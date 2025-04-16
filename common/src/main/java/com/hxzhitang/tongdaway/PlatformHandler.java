package com.hxzhitang.tongdaway;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * This class handles the registration of all content
 * Also handles making custom objects that are needed for each platforms
 * @author Joseph T. McQuigg
 */
public interface PlatformHandler {

    PlatformHandler PLATFORM_HANDLER = load(PlatformHandler.class);

    /**
     * Gets the path to the config directory
     * @return The path to the config directory
     */
    Path configPath();

    /**
     * Registers a block entity with the specified parameters
     * @see BlockEntityType
     * @param key The id/name of the block entity
     * @param builder The builder for the block entity
     * @return Supplier of the BlockEntityType
     */
    <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(String key, Supplier<BlockEntityType.Builder<T>> builder);

    private static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Common.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }

    default Supplier<Item> createBlockItem(Supplier<? extends Block> block, int burnTime) {
        return () -> new BlockItem(block.get(), new BlockItem.Properties());
    }

    <T> Supplier<T> register(Registry<? super T> registry, String name, Supplier<T> value);

    <T> Supplier<Holder.Reference<T>> registerForHolder(Registry<T> registry, String name, Supplier<T> value);
}
