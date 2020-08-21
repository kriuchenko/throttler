package com.throttler.map;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SlaState {
    private int rps;
    private long lastAccess;
    private final Set<String> tokens = ConcurrentHashMap.newKeySet();
    private final Queue<Long> accessTimes = new ConcurrentLinkedQueue<Long>();
    private long validAt;

    public SlaState(int rps, long now) {
        this.rps = rps;
        this.lastAccess = now;
    }

    public int getRps() {
        return rps;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public Set<String> getTokens() {
        return tokens;
    }

    public Queue<Long> getAccessTimes() {
        return accessTimes;
    }

    public boolean registerAccess(long now) {
        accessTimes.add(now);
        lastAccess = now;
        return true;
    }

    public long getValidAt() {
        return this.validAt;
    }
}
