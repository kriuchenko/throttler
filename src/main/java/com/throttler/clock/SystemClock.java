package com.throttler.clock;

public class SystemClock implements Clock {
    public long getMillis(){
        return System.currentTimeMillis();
    };
}
