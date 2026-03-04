package com.helloluckyhuang.tongdaway.structure;

import com.helloluckyhuang.tongdaway.TongDaWay;
import com.helloluckyhuang.tongdaway.util.VoxelGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class ModTemplate {
    protected final VoxelGrid voxelGrid;

    protected int heightOffset = 0;

    public ModTemplate(CompoundTag nbt) {
        this.voxelGrid = parseStructureNBT(nbt);
    }

    public int getWidth() {return voxelGrid.getWidth();}
    public int getHeight() {return voxelGrid.getHeight();}
    public int getDepth() {return voxelGrid.getDepth();}

    public int getUpperBound() {
        return voxelGrid.getHeight() + 1;
    }

    public int getLowerBound() {
        return - 1;
    }

    public abstract boolean isInVoxel(double x, double y, double z);

    public boolean isInVoxel(Vec3 pos) {
        return isInVoxel(pos.x(), pos.y(), pos.z());
    }

    public abstract BlockState getBlockState(double x, double y, double z);

    public BlockState getBlockState(Vec3 pos) {
        return getBlockState(pos.x(), pos.y(), pos.z());
    }

    /**
     * 解析结构 NBT 数据
     */
    private VoxelGrid parseStructureNBT(CompoundTag rootTag) {
        if (rootTag == null) return null;

        // 读取基本信息
        ListTag sizeTag = rootTag.getListOrEmpty("size");
        int x = sizeTag.getIntOr(0, 0), y = sizeTag.getIntOr(1, 0), z = sizeTag.getIntOr(2, 0);
        BlockPos size = new BlockPos(x, y, z);

        // 解析调色板
        List<BlockState> palette = parsePalette(rootTag.getListOrEmpty("palette"));

        // 解析方块数据并构建体素网格
        int[][][] voxelGrid = parseBlocks(rootTag.getListOrEmpty("blocks"), size);

        return new VoxelGrid(palette, voxelGrid, size);
    }

    /**
     * 解析调色板
     */
    private List<BlockState> parsePalette(ListTag paletteTag) {
        List<BlockState> palette = new ArrayList<>();

        // 索引 0 总是代表空位
        palette.add(Blocks.STRUCTURE_VOID.defaultBlockState());

        for (int i = 0; i < paletteTag.size(); i++) {
            CompoundTag blockTag = paletteTag.getCompoundOrEmpty(i);
            BlockState blockState = parseBlockState(blockTag);
            palette.add(blockState);
        }

        return palette;
    }

    /**
     * 解析单个方块状态
     */
    private BlockState parseBlockState(CompoundTag blockTag) {
        try {
            String blockName = blockTag.getStringOr("Name", "");
            ResourceLocation blockId = ResourceLocation.parse(blockName);
            Holder.Reference<Block> block = BuiltInRegistries.BLOCK.get(blockId).orElse(null);

            BlockState blockState;
            if (block != null)
                blockState = block.value().defaultBlockState();
            else
                blockState = Blocks.AIR.defaultBlockState();

            // 解析方块属性（如果有）
            if (blockTag.contains("Properties")) {
                CompoundTag propertiesTag = blockTag.getCompoundOrEmpty("Properties");
                blockState = applyProperties(blockState, propertiesTag);
            }

            return blockState;
        } catch (Exception e) {
            TongDaWay.LOGGER.error("Way parseBlockState Err: ", e);
            return Blocks.STONE.defaultBlockState();
        }
    }

    /**
     * 应用方块属性
     */
    private BlockState applyProperties(BlockState blockState, CompoundTag propertiesTag) {
        BlockState resultState = blockState;

        for (String propertyName : propertiesTag.keySet()) {
            String propertyValue = propertiesTag.getStringOr(propertyName, "");

            // 查找对应的方块属性
            Optional<Property<?>> property = findProperty(blockState, propertyName);

            if (property.isPresent()) {
                resultState = setPropertyValue(resultState, property.get(), propertyValue);
            }
        }

        return resultState;
    }

    /**
     * 查找方块属性
     */
    private Optional<Property<?>> findProperty(BlockState blockState, String propertyName) {
        return blockState.getProperties().stream()
                .filter(prop -> prop.getName().equals(propertyName))
                .findFirst();
    }

    /**
     * 设置方块属性值
     */
    private <T extends Comparable<T>> BlockState setPropertyValue(
            BlockState blockState, Property<T> property, String value) {

        Optional<T> propertyValue = property.getValue(value);
        if (propertyValue.isPresent()) {
            return blockState.setValue(property, propertyValue.get());
        } else {
            throw new IllegalArgumentException(
                    "无效的属性值: " + value + " 对于属性: " + property.getName());
        }
    }

    /**
     * 解析方块数据并构建体素网格
     */
    private int[][][] parseBlocks(ListTag blocksTag, BlockPos size) {
        int[][][] voxelGrid = new int[size.getX()][size.getY()][size.getZ()];

        // 初始化网格为 -1（空气）
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    voxelGrid[x][y][z] = 0;
                }
            }
        }

        // 填充实际方块
        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompoundOrEmpty(i);

            // 读取位置
            ListTag posTag = blockTag.getListOrEmpty("pos");
            int x = posTag.getIntOr(0, 0);
            int y = posTag.getIntOr(1, 0);
            int z = posTag.getIntOr(2, 0);

            // 读取调色板索引
            int state = blockTag.getIntOr("state", 0);

            // 确保位置在范围内
            if (x >= 0 && x < size.getX() && y >= 0 && y < size.getY() && z >= 0 && z < size.getZ()) {
                voxelGrid[x][y][z] = state + 1;
            }
        }

        return voxelGrid;
    }
}
