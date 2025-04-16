package com.hxzhitang.tongdaway.mixin;

import com.hxzhitang.tongdaway.way.ChunkGroup;
import com.hxzhitang.tongdaway.way.RegionPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static com.hxzhitang.tongdaway.Common.CHUNK_GROUP_SIZE;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin extends ChunkGeneratorMixin {
    @Shadow
    @Final
    private Holder<NoiseGeneratorSettings> settings;

    @Shadow @Final private Supplier<Aquifer.FluidPicker> globalFluidPicker;

    @Unique
    public final Map<RegionPos, Future<?>> tongDaWay$chunkGroupsFuture = new HashMap<>();
    @Unique
    public final Map<RegionPos, ChunkGroup> tongDaWay$chunkGroups = new HashMap<>();
    @Unique
    private final LinkedBlockingQueue<Runnable> tongDaWay$chunkGroupLoadQueue = new LinkedBlockingQueue<Runnable>();
    @Unique
    private final ThreadPoolExecutor tongDaWay$chunkGroupLoadPoolExecutor = new ThreadPoolExecutor(64, 1024, 1, TimeUnit.DAYS, tongDaWay$chunkGroupLoadQueue);

    @Inject(method = "buildSurface", at = @At("RETURN"))
    public void surfaceStart(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk, CallbackInfo ci) {
        var dimensionType = level.dimensionType();
        //只有主世界生成路
        if (dimensionType.effectsLocation().toString().equals("minecraft:overworld")) {
            int x = chunk.getPos().x;
            int z = chunk.getPos().z;
            int px = (int) Math.floor(x / (double) CHUNK_GROUP_SIZE);
            int pz = (int) Math.floor(z / (double) CHUNK_GROUP_SIZE);
            RegionPos regionPos = new RegionPos(px, pz);
            //如果本区块组没有生成过路线图
            if (!tongDaWay$ChunkWays.containsKey(regionPos)) {
                //创建本区域高度图生成线程
                if (!tongDaWay$chunkGroupsFuture.containsKey(regionPos)) {
                    ChunkGroup chunkGroup = new ChunkGroup(
                            px,
                            pz,
                            true,
                            level,
                            settings.value(),
                            random,
                            this::createStructures,
                            this::getNoiseChunk,
                            tongDaWay$ChunkWays
                    );
                    var f = tongDaWay$chunkGroupLoadPoolExecutor.submit(chunkGroup);
                    tongDaWay$chunkGroupsFuture.put(regionPos, f);
                    tongDaWay$chunkGroups.put(regionPos, chunkGroup);
                } else {
                    boolean isQuick = tongDaWay$chunkGroups.get(regionPos).isQuickLoadHeightMap();
                    if (!isQuick) {
                        tongDaWay$chunkGroups.get(regionPos).setLoadHeightMapQuickly();
                    }
                }
                try {
                    tongDaWay$chunkGroupsFuture.get(regionPos).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
            for (int px1 = px - 1; px1 <= px + 1; px1++) {
                for (int pz1 = pz - 1; pz1 <= pz + 1; pz1++) {
                    RegionPos regionPos1 = new RegionPos(px1, pz1);
                    if (!tongDaWay$ChunkWays.containsKey(regionPos1) && !tongDaWay$chunkGroupsFuture.containsKey(regionPos1)) {
                        ChunkGroup chunkGroup1 = new ChunkGroup(
                                px1,
                                pz1,
                                false,
                                level,
                                settings.value(),
                                random,
                                this::createStructures,
                                this::getNoiseChunk,
                                tongDaWay$ChunkWays
                        );
                        var f1 = tongDaWay$chunkGroupLoadPoolExecutor.submit(chunkGroup1);
                        tongDaWay$chunkGroupsFuture.put(regionPos1, f1);
                        tongDaWay$chunkGroups.put(regionPos1, chunkGroup1);
                    }
                }
            }
            if (tongDaWay$chunkGroupsFuture.containsKey(regionPos) && tongDaWay$chunkGroupsFuture.get(regionPos).isDone()) {
                tongDaWay$chunkGroupsFuture.remove(regionPos);
                tongDaWay$chunkGroups.remove(regionPos);
            }
        }
    }

    public NoiseChunk getNoiseChunk(ChunkPos chunkPos, RandomState randomState) {
        NoiseSettings noiseSettings = settings.value().noiseSettings();
        return new NoiseChunk(
                16 / noiseSettings.getCellWidth(),
                randomState,
                chunkPos.getMinBlockX(),
                chunkPos.getMinBlockZ(),
                noiseSettings,
                DensityFunctions.BeardifierMarker.INSTANCE,
                settings.value(),
                globalFluidPicker.get(),
                Blender.empty()
        );
    }
}
