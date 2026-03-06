package com.islesplus.entity;

import java.util.UUID;

public final class TrackedNode {
    public UUID textDisplayUuid;
    public String nodeName;
    public long firstSeenMs;
    public boolean locked;
    public boolean armed;
    public int previousCount = -1;
    public int peakCount = -1;
    public boolean wasDepleted;
    public boolean sawSelfFarming;
    public double nodeX;
    public double nodeY;
    public double nodeZ;

    public void resetFor(NodeSnapshot snapshot, long now) {
        textDisplayUuid = snapshot.uuid;
        nodeName = snapshot.nodeName;
        firstSeenMs = now;
        locked = false;
        armed = false;
        previousCount = snapshot.count;
        peakCount = snapshot.count;
        wasDepleted = snapshot.depleted;
        sawSelfFarming = false;
        nodeX = snapshot.entityX;
        nodeY = snapshot.entityY;
        nodeZ = snapshot.entityZ;
    }
}
