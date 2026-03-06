package com.islesplus.entity;

import java.util.UUID;

public final class NodeSnapshot {
    public final UUID uuid;
    public final String nodeName;
    public final String normalizedText;
    public final int count;
    public final boolean depleted;
    public final double distanceSq;
    public final double entityX;
    public final double entityY;
    public final double entityZ;

    public NodeSnapshot(UUID uuid, String nodeName, String normalizedText, int count, boolean depleted, double distanceSq, double entityX, double entityY, double entityZ) {
        this.uuid = uuid;
        this.nodeName = nodeName;
        this.normalizedText = normalizedText;
        this.count = count;
        this.depleted = depleted;
        this.distanceSq = distanceSq;
        this.entityX = entityX;
        this.entityY = entityY;
        this.entityZ = entityZ;
    }
}
