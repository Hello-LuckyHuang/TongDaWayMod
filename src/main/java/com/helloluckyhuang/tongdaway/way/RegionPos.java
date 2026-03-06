package com.helloluckyhuang.tongdaway.way;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.NotNull;

public record RegionPos(int x, int z) {
    public ListTag toNBT() {
        ListTag listTag = new ListTag();
        listTag.add(IntTag.valueOf(x));
        listTag.add(IntTag.valueOf(z));
        return listTag;
    }

    public static RegionPos fromNBT(ListTag listTag) {
        return new RegionPos(listTag.getIntOr(0, 0), listTag.getIntOr(1, 1));
    }

    @Override
    public @NotNull String toString() {
        return x + "_" + z;
    }

    @Override
    public int hashCode() {
        return x * 31 + z;
    }
}
