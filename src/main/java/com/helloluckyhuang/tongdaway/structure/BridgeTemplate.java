package com.helloluckyhuang.tongdaway.structure;

import net.minecraft.nbt.CompoundTag;

public class BridgeTemplate extends RoadTemplate {
    private final int deckStart;
    private final int deckEnd;
    private final int heightOffset;

    public BridgeTemplate(CompoundTag nbt, int deckStart, int deckEnd, int heightOffset) {
        super(nbt);
        this.deckStart = deckStart;
        this.deckEnd = deckEnd;
        this.heightOffset = heightOffset;
    }

    @Override
    public int getUpperBound() {
        return voxelGrid.getHeight() - heightOffset + 1;
    }

    @Override
    public int getLowerBound() {
        return -(heightOffset + 1);
    }
}
