package com.hxzhitang.tongdaway.util;

import com.hxzhitang.tongdaway.util.blocks.ModBlockEntities;
import com.hxzhitang.tongdaway.util.blocks.SignNotesSetBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.hxzhitang.tongdaway.Tongdaway.MODID;

public class RegistryHandler {
    //注册方块
    public static final DeferredRegister<Block> BLOCK = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    //初始化
    public static void init() {
        //注册方块
        BLOCK.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModBlockEntities.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    //方块
    public static final RegistryObject<Block> SIGN_NOTES_SET_BLOCK = BLOCK.register("sign_notes_set_block", ()->new SignNotesSetBlock(BlockBehaviour.Properties.of().strength(0.5F).lightLevel((l)->14).sound(SoundType.GLASS)));
    public static final RegistryObject<Item> SIGN_NOTES_SET_BLOCK_ITEM = ITEMS.register("sign_notes_set_block", () -> new BlockItem(SIGN_NOTES_SET_BLOCK.get(), new Item.Properties()));
}
