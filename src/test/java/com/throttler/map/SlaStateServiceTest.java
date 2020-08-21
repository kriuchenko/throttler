package com.throttler.map;

import com.throttler.clock.FrozenClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlaStateServiceTest {
    FrozenClock clock;
    SlaStateService slaStateService;

    @BeforeEach
    void setUp() {
        clock = new FrozenClock(0);
        slaStateService = new SlaStateService(clock);
    }

    @Test
    void tryAccess_given0Sla_shouldReturnFalse() {
        SlaState slaState = new SlaState(0, clock.getMillis());
        assertFalse(slaStateService.tryAccess(slaState));
    }

    @Test
    void tryAccess_given1Sla_when1Rps_shouldReturnTrue() {
        SlaState slaState = new SlaState(1, clock.getMillis());
        slaStateService.tryAccess(slaState);
        clock.ahead(1000);
        assertTrue(slaStateService.tryAccess(slaState));
    }

    @Test
    void tryAccess_given1Sla_when2Rps_shouldReturnFalse() {
        SlaState slaState = new SlaState(1, clock.getMillis());
        slaStateService.tryAccess(slaState);
        clock.ahead(100);
        assertFalse(slaStateService.tryAccess(slaState));
    }
}