package com.throttler.map;

import com.throttler.clock.Clock;

import java.util.Iterator;

public class SlaStateService {
    private final Clock clock;

    public SlaStateService(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAccess(SlaState slaState) {
        if (slaState.getRps() <= 0)
            return false;
        long now = clock.getMillis();
        if (now - slaState.getLastAccess() > 1000) {
            slaState.getAccessTimes().clear();
            return slaState.registerAccess(now);
        }
        removeOldAccessTimes(now, slaState);
        if (slaState.getAccessTimes().size() >= slaState.getRps())
            return false;
        return slaState.registerAccess(now);
    }

    private void removeOldAccessTimes(long now, SlaState slaState) {
        Iterator<Long> i = slaState.getAccessTimes().iterator();
        while (i.hasNext() && now - i.next() >= 1000)
            i.remove();
    }
}
