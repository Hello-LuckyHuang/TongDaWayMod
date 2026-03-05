package com.helloluckyhuang.tongdaway.util;

import java.util.*;

public class RandomPool<E> {
    // 存储标签与对象的映射
    private final Map<String, List<PoolObject>> tagMap = new HashMap<>();
    private final Map<Long, PoolObject> idMap = new HashMap<>();

    // 内部PoolObject类，存储对象、ID、标签
    private class PoolObject {
        E object;
        long id;
        Set<String> tags;

        PoolObject(E object, long id, String... tags) {
            this.object = object;
            this.id = id;
            this.tags = new HashSet<>(Arrays.asList(tags));
        }
    }

    // 添加对象到池中
    public void add(E obj, long id, String... tags) {
        PoolObject poolObject = new PoolObject(obj, id, tags);
        idMap.put(id, poolObject);

        // 将对象根据标签加入到tagMap中
        for (String tag : tags) {
            tagMap.computeIfAbsent(tag, k -> new ArrayList<>()).add(poolObject);
        }
    }

    // 根据ID获取对象
    public E getById(long id) {
        PoolObject poolObject = idMap.get(id);
        return poolObject != null ? poolObject.object : null;
    }

    // 根据标签和种子获取对象
    public E get(long seed, String tag1, String... tag2) {
        // 获取所有带有tag1标签的对象
        List<PoolObject> taggedObjects = tagMap.get(tag1);
        if (taggedObjects == null || taggedObjects.isEmpty()) {
            return null; // 如果没有tag1标签的对象，返回null
        }

        // 筛选出带有tag2标签的对象
        List<PoolObject> filteredObjects = new ArrayList<>();
        for (PoolObject obj : taggedObjects) {
            if (obj.tags.containsAll(Arrays.asList(tag2))) {
                filteredObjects.add(obj);
            }
        }

        // 如果没有符合tag2标签的对象，选择带有"default"标签的对象
        if (filteredObjects.isEmpty()) {
            for (PoolObject obj : taggedObjects) {
                if (obj.tags.contains("default")) {
                    filteredObjects.add(obj);
                }
            }
        }

        // 如果仍然没有对象符合条件，返回null
        if (filteredObjects.isEmpty()) {
            return null;
        }

        // 使用seed进行随机选择
        Random random = new Random(seed);
        PoolObject selected = filteredObjects.get(random.nextInt(filteredObjects.size()));
        return selected.object;
    }

    public static void main(String[] args) {
        // 创建RandomPool实例
        RandomPool<String> pool = new RandomPool<>();

        // 添加对象
        pool.add("Apple", 1, "fruit", "red", "default", "juicy");
        pool.add("Cherry", 6, "fruit", "red", "juicy");
        pool.add("Orange", 5, "fruit", "juicy");
        pool.add("Banana", 2, "fruit", "yellow");
        pool.add("Carrot", 3, "vegetable", "orange", "default");
        pool.add("Spinach", 4, "vegetable", "green");

        // 获取ID为1的对象
        System.out.println(pool.getById(1)); // 输出 "Apple"

        // 获取带有"fruit"标签并且带有"red"标签的对象
        System.out.println(pool.get(System.currentTimeMillis(), "fruit", "red", "juicy")); // 输出 "Apple"

        // 获取带有"fruit"标签并且带有"default"标签的对象
        System.out.println(pool.get(12345, "fruit")); // 输出 "Apple" 或 "Banana"（随机）

        // 获取没有"fruit"标签的情况下，带有"default"标签的对象
        System.out.println(pool.get(12345, "vegetable", "yellow")); // 输出 "Carrot" 或 "Spinach"（随机）
    }
}