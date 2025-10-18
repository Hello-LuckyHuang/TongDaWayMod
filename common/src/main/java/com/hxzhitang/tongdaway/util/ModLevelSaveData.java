package com.hxzhitang.tongdaway.util;

import com.hxzhitang.tongdaway.way.RegionPos;
import com.hxzhitang.tongdaway.way.RegionWayMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.concurrent.ConcurrentHashMap;

/**
 * ModLevelSaveData�����ڹ���ͳ־û���Ϸ�е�·�߼������ݡ�
 * ����Ԥ�����·�߱����ڴ����У��Ա�����Ҫʱ���ټ��ء�
 * �������Ա�������Ϸ����ʱ�ظ�����·�ߣ������Ϸ���ܡ�
 */
public class ModLevelSaveData extends SavedData {
    public static final String NAME = "tongdaway_mod_way_data";
    protected final ConcurrentHashMap<RegionPos, RegionWayMap> chunkWays = new ConcurrentHashMap<>();

    public static ModLevelSaveData create() {
        return new ModLevelSaveData();
    }

    public void putRegionWay(RegionPos regionPos, RegionWayMap regionWayMap) {
        chunkWays.put(regionPos, regionWayMap);
        setDirty();
    }

    public RegionWayMap getRegionWay(RegionPos regionPos) {
        setDirty();
        return chunkWays.get(regionPos);
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        ListTag listTag = new ListTag();
        chunkWays.forEach((pos, wayMap) -> {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.put("RegionX", IntTag.valueOf(pos.x()));
            compoundTag.put("RegionZ", IntTag.valueOf(pos.z()));
            compoundTag.put("WayMap", wayMap.getTag());
            listTag.add(compoundTag);
        });
        pCompoundTag.put("way_data", listTag);
        return pCompoundTag;
    }

    public static ModLevelSaveData load(CompoundTag nbt) {
        ModLevelSaveData data = ModLevelSaveData.create();
        ListTag listNBT = (ListTag) nbt.get("way_data");
        if (listNBT != null) {
            for (Tag value : listNBT) {
                CompoundTag tag = (CompoundTag) value;
                int regionX = tag.getInt("RegionX");
                int regionZ = tag.getInt("RegionZ");
                RegionPos regionPos = new RegionPos(regionX, regionZ);
                ListTag wayMapListTag = (ListTag) tag.get("WayMap");
                RegionWayMap regionWayMap;
                if (wayMapListTag != null) {
                    regionWayMap = RegionWayMap.loadTag(regionX, regionZ, wayMapListTag);
                    data.chunkWays.put(regionPos, regionWayMap);
                }
            }
        }
        return data;
    }

    /**
     * ��ȡָ�������ModLevelSaveDataʵ����ͨ�����������ö�Ӧ��data
     *
     * @param worldIn Ҫ��ȡ���ݵ����硣
     * @return ��ָ�����������ModLevelSaveDataʵ����
     * @throws RuntimeException ������Դӿͻ��������ȡ���ݣ����׳�����ʱ�쳣��
     * **/
    public static ModLevelSaveData get(Level worldIn) {
        if (!(worldIn instanceof ServerLevel)) {
            throw new RuntimeException("Attempted to get the data from a client world. This is wrong.");
        }
        ServerLevel world = worldIn.getServer().getLevel(ServerLevel.OVERWORLD);
        DimensionDataStorage dataStorage = world.getDataStorage();
        return dataStorage.computeIfAbsent(ModLevelSaveData::load, ModLevelSaveData::create, ModLevelSaveData.NAME);
    }
}