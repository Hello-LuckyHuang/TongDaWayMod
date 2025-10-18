package com.hxzhitang.tongdaway;

import com.google.gson.annotations.SerializedName;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config {
    // ·������������������
    @SerializedName("features")
    private List<String> features = List.of("minecraft:village"); // Ĭ��ֵ

    // ·�Ƿ������Ӵ�ׯ
    @SerializedName("connectVillage")
    private boolean connectVillage = true; // Ĭ��ֵ

    // һ�������������������������
    @SerializedName("connectFeaturesNum")
    private int connectFeaturesNum = 3; // Ĭ��ֵ

    // ·���ϵ�������Ϣ
    @SerializedName("notes")
    private List<String> notes = List.of("[HELLO]Welcome to Minecraft World!"); // Ĭ��ֵ

    // Getter��Setter����
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
