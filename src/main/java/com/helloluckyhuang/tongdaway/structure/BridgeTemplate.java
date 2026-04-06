package com.helloluckyhuang.tongdaway.structure;

import net.minecraft.nbt.CompoundTag;

public class BridgeTemplate extends RoadTemplate {
    private final int deckStart;
    private final int deckEnd;

    public BridgeTemplate(CompoundTag nbt, int deckStart, int deckEnd, int heightOffset) {
        super(nbt, heightOffset);
        this.deckStart = deckStart;
        this.deckEnd = deckEnd;
    }

    @Override
    public int getUpperBound() {
        return voxelGrid.getHeight() - heightOffset + 1;
    }

    @Override
    public int getLowerBound() {
        return -(heightOffset + 1);
    }

    public int getDeckStart() {
        return deckStart;
    }

    public int getDeckEnd() {
        return voxelGrid.getWidth() - deckEnd;
    }

    public int getDeckLength() {
        return deckEnd - deckStart + 1;
    }

    public int getTotalLength() {
        return voxelGrid.getWidth();
    }
}
