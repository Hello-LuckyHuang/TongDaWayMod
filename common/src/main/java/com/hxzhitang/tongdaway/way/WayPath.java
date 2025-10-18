package com.hxzhitang.tongdaway.way;

import java.util.List;

public class WayPath {
    public List<int[]> points;
    public String wayName;
    public WayPath(List<int[]> points, String wayName) {
        this.points = points;
        this.wayName = wayName;
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }
}
