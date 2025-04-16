package com.hxzhitang.tongdaway.util.blocks;

import com.hxzhitang.tongdaway.PlatformHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class ModBlocks {
    public static void loadClass() {
    }
    public static final Supplier<Block> SIGN_NOTES_SET_BLOCK = PlatformHandler.PLATFORM_HANDLER.register(BuiltInRegistries.BLOCK, "sign_notes_set_block", ()->new SignNotesSetBlock(BlockBehaviour.Properties.of().strength(0.5F).lightLevel((l)->14).sound(SoundType.GLASS)));
    public static final Supplier<Item> SIGN_NOTES_SET_BLOCK_ITEM = PlatformHandler.PLATFORM_HANDLER.register(BuiltInRegistries.ITEM, "sign_notes_set_block", () -> new BlockItem(SIGN_NOTES_SET_BLOCK.get(), new Item.Properties()));
}
