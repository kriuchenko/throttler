package com.throttler.map;

import com.throttler.clock.Clock;
import com.throttler.sla.SlaService;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class UserService {
    private final AtomicLong oldestSlaStateTime = new AtomicLong(0);
    private final int slaTtl;
    private final Map<String, SlaState> slaStates = new LinkedHashMap<>(16, .75f, true);
    private final ReadWriteLock slaStatesLock = new ReentrantReadWriteLock();
    private final SlaState guestData;
    private final Clock clock;

    public UserService(int guestRps, int slaTtl, Clock clock) {
        this.slaTtl = slaTtl;
        this.clock = clock;
        guestData = new SlaState(guestRps, clock.getMillis());
    }

    public boolean isCleanupRequired() {
        return clock.getMillis() - oldestSlaStateTime.get() > slaTtl;
    }

    public SlaState getGuestSlaState() {
        return guestData;
    }

    public void setSlaState(String token, SlaService.SLA sla){
        /* TODO Using single writeLock or a combination of readLock+writeLock is driven by the
        number of simultaneous tokens typically used within SLA cache TTL - low number makes writeLock
        beneficial, while readLock+writeLock wins for high number
         */
        SlaState slaState = withLock(slaStatesLock.writeLock(), () ->
            Optional.ofNullable(slaStates.get(sla.getUser())).orElseGet(() -> {
                SlaState newSlaState = new SlaState(sla.getRps(), clock.getMillis());
                slaStates.put(sla.getUser(), newSlaState);
                return newSlaState;
            })
        );
        slaState.getTokens().add(token);
    }

    public boolean hasOutdatedSlaStates(){
        return withLock(slaStatesLock.readLock(), () -> {
            Iterator<SlaState> iterator = slaStates.values().iterator();
            return iterator.hasNext() && clock.getMillis() - iterator.next().getLastAccess() > slaTtl;
        });
    }

    public Collection<String> removeOldSlaStates(){
        Collection<String> droppedTokens = new LinkedList<>();
        long now = clock.getMillis();
        return withLock(slaStatesLock.writeLock(), () -> {
            Iterator<SlaState> iterator = slaStates.values().iterator();
            while(iterator.hasNext()) {
                SlaState slaState = iterator.next();
                oldestSlaStateTime.set(slaState.getLastAccess());
                if(now - slaState.getLastAccess() >= slaTtl) {
                    droppedTokens.addAll(slaState.getTokens());
                    iterator.remove();
                } else
                    return droppedTokens;
            }
            return droppedTokens;
        });
    }

    public Optional<SlaState> getSlaState(String user) {
        return withLock(slaStatesLock.readLock(), () -> Optional.ofNullable(slaStates.get(user)));
    }

    private <T> T withLock(Lock lock, Supplier<T> supplier){
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }
}
