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
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

//@Mod.EventBusSubscriber(modid = Tongdaway.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEventTest {
//    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("testmodtest")
                        .executes(context -> generateStructure(
                                context.getSource().getPlayerOrException(),
                                new ResourceLocation(Tongdaway.MODID, "intersection/testmodstructure")
                        ))
        );
    }

    private static int generateStructure(Player player, ResourceLocation structureId) {
        if (player.level() instanceof ServerLevel serverLevel) {
//            var c = serverLevel.getChunkSource().getChunk(100, 100, ChunkStatus.EMPTY, true);
//            int h = c.getHeight(Heightmap.Types.WORLD_SURFACE_WG, 5, 5);
//            System.out.println("=======> " + h);
//            var c2 = serverLevel.getChunkSource().getChunk(100, 100, ChunkStatus.FULL, true);
//            int h2 = c2.getHeight(Heightmap.Types.WORLD_SURFACE_WG, 5, 5);
//            System.out.println("=======> " + h2);
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

//import net.minecraft.server.world.ServerChunkManager;
//import net.minecraft.server.world.ServerWorld;
//import net.minecraft.util.math.ChunkPos;
//import net.minecraft.world.chunk.Chunk;
//import net.minecraft.world.chunk.ChunkStatus;
//
//import java.util.concurrent.CompletableFuture;
//
//public class VanillaChunkGeneration {
//
//    /**
//     * 使用原版函数将区块生成到 LIGHT 状态
//     *
//     * @param world 服务器世界
//     * @param chunkX 区块 X 坐标
//     * @param chunkZ 区块 Z 坐标
//     * @return CompletableFuture,完成时返回生成的区块
//     */
//    public static CompletableFuture<Chunk> generateChunkToLight(
//            ServerWorld world, int chunkX, int chunkZ) {
//
//        ServerChunkManager chunkManager = world.getChunkManager();
//
//        // 使用原版的 getChunk 方法,指定 ChunkStatus.LIGHT
//        // create=true 表示如果区块不存在则生成
//        Chunk chunk = chunkManager.getChunk(chunkX, chunkZ, ChunkStatus.LIGHT, true);
//
//        // 如果需要异步获取,可以使用内部方法
//        // 注意:这需要通过 mixin 访问
//        return CompletableFuture.completedFuture(chunk);
//    }
//
//    /**
//     * 异步版本 - 使用原版的异步 API
//     */
//    public static CompletableFuture<Chunk> generateChunkToLightAsync(
//            ServerWorld world, int chunkX, int chunkZ) {
//
//        ServerChunkManager chunkManager = world.getChunkManager();
//
//        // 通过 mixin 访问内部的异步方法
//        // 这会返回一个 CompletableFuture<OptionalChunk<Chunk>>
//        return ((IServerChunkManager) chunkManager)
//                .invokeGetChunkFuture(chunkX, chunkZ, ChunkStatus.LIGHT, true)
//                .thenApply(optionalChunk -> optionalChunk.orElse(null));
//    }
//
//    /**
//     * 使用示例
//     */
//    public static void example(ServerWorld world) {
//        ChunkPos pos = new ChunkPos(10, 20);
//
//        // 同步方式
//        Chunk chunk = generateChunkToLight(world, pos.x, pos.z).join();
//        System.out.println("区块已生成到 LIGHT 状态: " + chunk.getStatus());
//
//        // 异步方式
//        generateChunkToLightAsync(world, pos.x, pos.z)
//                .thenAccept(c -> {
//                    if (c != null) {
//                        System.out.println("异步生成完成: " + c.getStatus());
//                    }
//                });
//    }
//}