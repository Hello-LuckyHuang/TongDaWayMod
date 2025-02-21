package com.hxzhitang.tongdaway.util.blocks;

import com.hxzhitang.tongdaway.util.RegistryHandler;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static com.hxzhitang.tongdaway.Tongdaway.MODID;
import static net.minecraftforge.registries.ForgeRegistries.Keys.BLOCK_ENTITY_TYPES;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BLOCK_ENTITY_TYPES, MODID);

    public static final RegistryObject<BlockEntityType<SignNotesSetBlockEntity>> SIGN_NOTES_SET =
            BLOCK_ENTITIES.register("sign_notes_set_block",
                    ()->BlockEntityType.Builder.of(SignNotesSetBlockEntity::new, RegistryHandler.SIGN_NOTES_SET_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus){
        BLOCK_ENTITIES.register(eventBus);
    }
}
