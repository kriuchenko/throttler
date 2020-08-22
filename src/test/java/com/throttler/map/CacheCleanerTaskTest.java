package com.throttler.map;

import com.throttler.clock.FrozenClock;
import com.throttler.sla.SlaService;
import com.throttler.sla.SlaServiceMock;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;

import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

class CacheCleanerTaskTest {
    private static final int GUEST_RPS = 0;
    private static final int SLA_TTL = 1000;
    FrozenClock clock = new FrozenClock(0);
    private UserService userService;
    private TokenService tokenService;
    private final SlaService slaService = new SlaServiceMock();

    @BeforeEach
    void setUp() {
        tokenService = Mockito.spy(new TokenService(slaService));
        userService = Mockito.spy(new UserService(GUEST_RPS, SLA_TTL, clock));
    }

    @Test
    void run_whenOutdatedRecordsPresent_shouldRemoveOutdated() {
        String token = "a";
        userService.setSlaState(token, new SlaService.SLA("A", 1));
        clock.ahead(SLA_TTL + 1);
        CacheCleanerTask task = new CacheCleanerTask(userService, tokenService);
        task.run();
        verify(userService).hasOutdatedSlaStates();
        verify(userService).removeOldSlaStates();
        verify(tokenService).removeTokens((Collection<String>) argThat(Matchers.contains(token)));
    }

    @Test
    void run_whenCurrentRecordsPresent_shouldRemoveNothing() {
        String token = "a";
        userService.setSlaState(token, new SlaService.SLA("A", 1));
        CacheCleanerTask task = new CacheCleanerTask(userService, tokenService);
        task.run();
        verify(userService).hasOutdatedSlaStates();
        verify(userService, never()).removeOldSlaStates();
        verify(tokenService, never()).removeTokens((Collection<String>) argThat(Matchers.contains(token)));
    }
}