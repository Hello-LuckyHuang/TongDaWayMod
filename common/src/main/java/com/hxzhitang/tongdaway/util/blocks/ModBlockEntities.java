package com.hxzhitang.tongdaway.util.blocks;

import com.hxzhitang.tongdaway.PlatformHandler;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static void loadClass() {
    }
    public static final Supplier<BlockEntityType<SignNotesSetBlockEntity>> SIGN_NOTES_SET =
            PlatformHandler.PLATFORM_HANDLER.registerBlockEntity(
                    "sign_notes_set_block",
                    () -> BlockEntityType.Builder.of(
                            SignNotesSetBlockEntity::new,
                            ModBlocks.SIGN_NOTES_SET_BLOCK.get()
                    )//.build(null)
            );
}
