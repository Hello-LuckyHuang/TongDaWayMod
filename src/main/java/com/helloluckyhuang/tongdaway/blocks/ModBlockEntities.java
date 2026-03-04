package com.helloluckyhuang.tongdaway.blocks;

import com.helloluckyhuang.tongdaway.TongDaWay;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


public class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TongDaWay.MODID);
    public static final Supplier<BlockEntityType<TrackSpawnerBlockEntity>> TRACK_SPAWNER =
            BLOCK_ENTITIES.register("track_spawner", () -> new BlockEntityType<>(TrackSpawnerBlockEntity::new, ModBlocks.TRACK_SPAWNER.get()));

    public static void register(IEventBus eventBus){
        BLOCK_ENTITIES.register(eventBus);
    }
}
