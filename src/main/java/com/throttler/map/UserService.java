package com.throttler.map;

import com.throttler.clock.Clock;
import com.throttler.sla.SlaService;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class UserService {
    private long oldestSlaStateTime = 0;
    private final Map<String, SlaState> slaStates = new LinkedHashMap<>();
    private final ReadWriteLock slaStatesLock = new ReentrantReadWriteLock();
    private final SlaState guestData;
    private final Clock clock;

    public UserService(int guestRps, Clock clock) {
        this.clock = clock;
        guestData = new SlaState(guestRps, clock.getMillis());
    }

    public long getOldestSlaStateTime() {
        return oldestSlaStateTime;
    }

    public SlaState getGuestSlaState() {
        return guestData;
    }

    public void setSlaState(String token, SlaService.SLA sla){
        /* TODO Using single writeLock or a combination of readLock+writeLock is driven by the
        number of simulteneous tokens typically used within SLA cache TTL - low number makes writeLock
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

    public boolean hasOutdatedSlaStates(long ttl){
        return withLock(slaStatesLock.readLock(), () -> {
            Iterator<SlaState> iterator = slaStates.values().iterator();
            return iterator.hasNext() && clock.getMillis() - iterator.next().getLastAccess() >= ttl;
        });
    }

    public Collection<String> removeOldSlaStates(long ttl){
        Collection<String> droppedTokens = new LinkedList<>();
        long now = clock.getMillis();
        return withLock(slaStatesLock.writeLock(), () -> {
            Iterator<SlaState> iterator = slaStates.values().iterator();
            while(iterator.hasNext()) {
                SlaState slaState = iterator.next();
                oldestSlaStateTime = slaState.getLastAccess();
                if(now - slaState.getLastAccess() >= ttl) {
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
