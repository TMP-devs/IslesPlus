package com.islesplus.features.plushiefinder;

public final class PlushieEntry {
    public final int num;
    public final double xReal, yReal, zReal;
    public final Double xEntrance, yEntrance, zEntrance;

    public PlushieEntry(int num, double xReal, double yReal, double zReal,
                        Double xEntrance, Double yEntrance, Double zEntrance) {
        this.num = num;
        this.xReal = xReal;
        this.yReal = yReal;
        this.zReal = zReal;
        this.xEntrance = xEntrance;
        this.yEntrance = yEntrance;
        this.zEntrance = zEntrance;
    }

    public boolean hasEntrance() {
        return xEntrance != null && yEntrance != null && zEntrance != null;
    }
}
