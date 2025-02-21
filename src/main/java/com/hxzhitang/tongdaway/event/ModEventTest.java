package com.hxzhitang.tongdaway.event;

import com.hxzhitang.tongdaway.Tongdaway;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Tongdaway.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEventTest {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("generatestructure")
                        .executes(context -> generateStructure(
                                context.getSource().getPlayerOrException(),
                                new ResourceLocation(Tongdaway.MODID, "intersection/testmodstructure")
                        ))
        );
    }

    private static int generateStructure(Player player, ResourceLocation structureId) {
        if (player.level() instanceof ServerLevel serverLevel) {
//            // 获取结构模板管理器
//            StructureTemplateManager manager = serverLevel.getStructureManager();
//
//            manager.get(structureId).ifPresent(template -> {
//                BlockPos centerPos = player.blockPosition();
//
//                // 计算结构起点（默认以结构中心对齐玩家位置）
//                BlockPos cornerPos = centerPos.offset(
//                        -template.getSize().getX() / 2,
//                        0,
//                        -template.getSize().getZ() / 2
//                );
//
//                // 生成结构
//                template.placeInWorld(
//                        serverLevel,
//                        cornerPos,
//                        cornerPos,
//                        new StructurePlaceSettings()
//                                .setRotation(Rotation.NONE)
//                                .setMirror(Mirror.NONE)
//                                .setIgnoreEntities(false),
//                        serverLevel.random,
//                        Block.UPDATE_CLIENTS
//                );
//            });
            BlockPos centerPos = player.blockPosition();
            serverLevel.setBlock(centerPos, Blocks.OAK_SIGN.defaultBlockState(), 0);
            BlockEntity blockEntity = serverLevel.getBlockEntity(centerPos);
            if (blockEntity instanceof SignBlockEntity sign) {
                // 修改告示牌内容
                sign.setText(sign.getFrontText().setMessage(0, Component.literal("hello")), true);
            }
        }
        return 1;
    }
}
