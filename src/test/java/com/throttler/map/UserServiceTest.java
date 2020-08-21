package com.throttler.map;

import com.throttler.clock.FrozenClock;
import com.throttler.sla.SlaService;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    private static final int GUEST_RPS = 0;
    private static final String TOKEN = "a";
    private static final String USER = "A";
    FrozenClock clock = new FrozenClock(0);
    UserService userService = new UserService(GUEST_RPS, clock);

    @Test
    void getGuestData_shouldHaveGuestRps() {
        assertEquals(GUEST_RPS, userService.getGuestSlaState().getRps());
    }

    @Test
    void setSlaState_givenNoSlaState_shouldAddSlaState() {
        SlaService.SLA sla = new SlaService.SLA(USER, 1);
        userService.setSlaState(TOKEN, sla);
        Optional<SlaState> slaState = userService.getSlaState(sla.getUser());
        assertTrue(slaState.isPresent());
        assertEquals(sla.getRps(), slaState.get().getRps());
    }

    @Test
    void setSlaState_givenSlaState_shouldAddToken() {
        SlaService.SLA sla = new SlaService.SLA(USER, 1);
        userService.setSlaState(TOKEN, sla);
        userService.setSlaState("B", sla);
        Optional<SlaState> slaState = userService.getSlaState(sla.getUser());
        assertTrue(slaState.isPresent());
        assertEquals(Set.of(TOKEN, "B"), slaState.get().getTokens());
    }

    @Test
    void hasOutdatedSlaStates_givenOldStates_shouldReturnTrue() {
        userService.setSlaState(TOKEN, new SlaService.SLA(USER, 1));
        clock.ahead(100);
        assertTrue(userService.hasOutdatedSlaStates(100));
    }

    @Test
    void hasOutdatedSlaStates_givenFreshStates_shouldReturnFalse() {
        userService.setSlaState(TOKEN, new SlaService.SLA(USER, 1));
        clock.ahead(100);
        assertFalse(userService.hasOutdatedSlaStates(1000));
    }

    @Test
    void dropOldSlaStates_shouldRemoveOldStatesAndKeepCurrent() {
        userService.setSlaState(TOKEN, new SlaService.SLA(USER, 1));
        clock.ahead(1000);
        userService.setSlaState("b", new SlaService.SLA("B", 1));
        userService.removeOldSlaStates(1000);
        assertTrue(userService.getSlaState(USER).isEmpty(), "Old user was not removed");
        assertFalse(userService.getSlaState("B").isEmpty(), "Current user was removed");
    }

    @Test
    void dropOldSlaStates_shouldReturnDroppedTokens() {
        userService.setSlaState("a", new SlaService.SLA("A", 1));
        userService.setSlaState("b", new SlaService.SLA("A", 1));
        clock.ahead(1000);
        userService.setSlaState("c", new SlaService.SLA("C", 1));
        Collection<String> droppedTokens = userService.removeOldSlaStates(1000);
        assertEquals(Set.of("a", "b"), Set.copyOf(droppedTokens));
    }
}