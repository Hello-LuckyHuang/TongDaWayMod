package com.hxzhitang.tongdaway;

import com.google.gson.annotations.SerializedName;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config {
    // 路可以连接特征的名单
    @SerializedName("features")
    private List<String> features = List.of("minecraft:village"); // 默认值

    // 路是否总连接村庄
    @SerializedName("connectVillage")
    private boolean connectVillage = true; // 默认值

    // 一个区域内连接特征的最大数量
    @SerializedName("connectFeaturesNum")
    private int connectFeaturesNum = 3; // 默认值

    // 路牌上的其他信息
    @SerializedName("notes")
    private List<String> notes = List.of("[HELLO]Welcome to Minecraft World!"); // 默认值

    // Getter和Setter方法
    public Set<String> getFeatures() {
        return new HashSet<>(features);
    }

    public boolean isConnectVillage() {
        return connectVillage;
    }

    public int getConnectFeaturesNum() {
        return connectFeaturesNum;
    }

    public Set<String> getNotes() {
        return new HashSet<>(notes);
    }
}
