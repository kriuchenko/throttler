package com.throttler.clock;

public class FrozenClock implements Clock {
    private long millis;

    public FrozenClock(long millis) {
        this.millis = millis;
    }

    public long getMillis(){
        return millis;
    };

    public void ahead(long delta) {
        millis += delta;
    }
}
