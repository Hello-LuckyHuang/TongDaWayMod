package com.helloluckyhuang.tongdaway.util;

import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;

public class Vec3Helper {
    public static ListTag toTag(Vec3 vec) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(vec.x));
        list.add(DoubleTag.valueOf(vec.y));
        list.add(DoubleTag.valueOf(vec.z));
        return list;
    }

    public static Vec3 fromTag(ListTag list) {
        double x = list.getDouble(0);
        double y = list.getDouble(1);
        double z = list.getDouble(2);
        return new Vec3(x, y, z);
    }
}
