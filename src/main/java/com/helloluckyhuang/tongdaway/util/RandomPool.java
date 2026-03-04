package com.helloluckyhuang.tongdaway.util;

import java.util.*;

public class RandomPool<E> {
    private static class PoolObject<E> {
        E obj;
        long id;
        Set<String> tags;

        public PoolObject(E obj, long id, Set<String> tags) {
            this.obj = obj;
            this.id = id;
            this.tags = tags;
        }
    }

    private final Map<Long, PoolObject<E>> objectById = new HashMap<>();
    private final Map<String, List<PoolObject<E>>> objectsByTag = new HashMap<>();

    // Add an object with tags and id
    public void add(E obj, long id, String... tags) {
        Set<String> tagSet = new HashSet<>(Arrays.asList(tags));
        PoolObject<E> poolObject = new PoolObject<>(obj, id, tagSet);

        // Store by ID
        objectById.put(id, poolObject);

        // Store by tags
        for (String tag : tags) {
            objectsByTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(poolObject);
        }
    }

    // Get a random object based on tags and seed
    public E get(long seed, String... tags) {
        // Collect potential objects based on tags
        List<PoolObject<E>> candidateObjects = new ArrayList<>();

        // Try to find the best match: most tags
        int maxTagCount = -1;
        for (String tag : tags) {
            List<PoolObject<E>> objects = objectsByTag.get(tag);
            if (objects != null) {
                for (PoolObject<E> poolObject : objects) {
                    int commonTagsCount = 0;
                    for (String tagInPool : poolObject.tags) {
                        if (Arrays.asList(tags).contains(tagInPool)) {
                            commonTagsCount++;
                        }
                    }
                    if (commonTagsCount > maxTagCount) {
                        candidateObjects.clear();
                        candidateObjects.add(poolObject);
                        maxTagCount = commonTagsCount;
                    } else if (commonTagsCount == maxTagCount) {
                        candidateObjects.add(poolObject);
                    }
                }
            }
        }

        // If no object found with the given tags, fallback to default
        if (candidateObjects.isEmpty()) {
            candidateObjects = objectsByTag.getOrDefault("default", new ArrayList<>());
        }

        // If there are still no objects, return null
        if (candidateObjects.isEmpty()) {
            return null;
        }

        // Use the seed to select a random object
        Random random = new Random(seed);
        int index = random.nextInt(candidateObjects.size());
        return candidateObjects.get(index).obj;
    }

    // Get an object by its id
    public E getById(long id) {
        PoolObject<E> poolObject = objectById.get(id);
        return poolObject != null ? poolObject.obj : null;
    }
}