package com.hxzhitang.tongdaway;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class Common {
    public static final String MODID = "tongdaway";
    public static final Logger LOGGER = LogUtils.getLogger();

    //以此为边长的区块组生成噪声图
    public static final int CHUNK_GROUP_SIZE = 32;

    public static ResourceLocation id(String name) {
        return new ResourceLocation(MODID, name);
    }
}