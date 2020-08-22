package com.throttler.map;

import com.throttler.ThrottlingService;
import com.throttler.clock.FrozenClock;
import com.throttler.sla.SlaService;
import com.throttler.sla.SlaServiceMock;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapBasedThrottlingServiceTest {
    private static final int SLA_TTL = 1000;
    private static final String USER_A = "A";
    private static final String USER_B = "B";
    FrozenClock clock = new FrozenClock(0);
    private UserService userService;
    private final SlaService slaService = new SlaServiceMock();
    private SlaConsumer slaConsumer;
    ThrottlingService throttlingService;
    private static final String USER_A_TOKEN = "a";
    private static final String USER_B_TOKEN = "b";

    private MapBasedThrottlingService buildThrottlingService(int guestRps, int slaTTl) {
        TokenService tokenService = new TokenService(slaService);
        this.userService = new UserService(guestRps, slaTTl, clock);
        SlaStateService slaStateService = new SlaStateService(clock);
        this.slaConsumer = new SlaConsumerImpl(userService, tokenService);
        return new MapBasedThrottlingService(slaTTl, tokenService, userService, slaStateService, clock);
    }

    @Test
    void isRequestAllowed_givenGuestRps0_whenGuestToken_shouldDeny() {
        throttlingService = buildThrottlingService(0, 1);
        assertFalse(throttlingService.isRequestAllowed(Optional.empty()));
    }

    @Test
    void isRequestAllowed_givenGuestRps0_whenUserToken_shouldDeny() {
        throttlingService = buildThrottlingService(0, 1);
        assertFalse(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
    }

    @Test
    void isRequestAllowed_givenGuestRps1_shouldAllow() {
        throttlingService = buildThrottlingService(1, 1);
        assertTrue(throttlingService.isRequestAllowed(Optional.empty()));
    }

    @Test
    void isRequestAllowed_givenGuestRps1_when2Requests_shouldDeny() {
        throttlingService = buildThrottlingService(1, 1);
        assertTrue(throttlingService.isRequestAllowed(Optional.empty()));
        assertFalse(throttlingService.isRequestAllowed(Optional.empty()));
    }

    @Test
    void isRequestAllowed_givenGuestRps1_when2RequestsIn2Seconds_shouldAllow() {
        throttlingService = buildThrottlingService(1, 1);
        assertTrue(throttlingService.isRequestAllowed(Optional.empty()));
        clock.ahead(SLA_TTL);
        assertTrue(throttlingService.isRequestAllowed(Optional.empty()));
    }

    @Test
    void isRequestAllowed_givenGuestRps1_when3RequestsIn2Seconds_shouldDenyLast() {
        throttlingService = buildThrottlingService(1, 1);
        assertTrue(throttlingService.isRequestAllowed(Optional.empty()));
        clock.ahead(SLA_TTL);
        assertTrue(throttlingService.isRequestAllowed(Optional.empty()));
        clock.ahead(SLA_TTL/2);
        assertFalse(throttlingService.isRequestAllowed(Optional.empty()));
    }

    @Test
    void isRequestAllowed_givenUserRps1_shouldAllow() {
        throttlingService = buildThrottlingService(0, 1);
        slaConsumer.accept(USER_A_TOKEN, new SlaService.SLA(USER_A, 1));
        assertTrue(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
    }

    @Test
    void isRequestAllowed_givenUserRps1_when2Requests_shouldDeny() {
        throttlingService = buildThrottlingService(1, 1);
        slaConsumer.accept(USER_A_TOKEN, new SlaService.SLA(USER_A, 1));
        assertTrue(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
        assertFalse(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
    }

    @Test
    void isRequestAllowed_givenUserRps1_when2RequestsIn2Seconds_shouldAllow() {
        throttlingService = buildThrottlingService(1, 1);
        slaConsumer.accept(USER_A_TOKEN, new SlaService.SLA(USER_A, 1));
        assertTrue(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
        clock.ahead(SLA_TTL);
        assertTrue(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
    }

    @Test
    void isRequestAllowed_givenUserARps1AndUserBRps0_whenRequestsFromUserA_shouldAllow() {
        throttlingService = buildThrottlingService(1, 1);
        slaConsumer.accept(USER_A_TOKEN, new SlaService.SLA(USER_A, 1));
        slaConsumer.accept(USER_B_TOKEN, new SlaService.SLA(USER_B, 0));
        assertTrue(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
    }

    @Test
    void isRequestAllowed_givenUserARps1AndUserBRps0_whenRequestsFromUserB_shouldDeny() {
        throttlingService = buildThrottlingService(1, 1);
        slaConsumer.accept(USER_A_TOKEN, new SlaService.SLA(USER_A, 1));
        slaConsumer.accept(USER_B_TOKEN, new SlaService.SLA(USER_B, 0));
        assertFalse(throttlingService.isRequestAllowed(Optional.of(USER_B_TOKEN)));
    }

    @Test
    void isRequestAllowed_shouldRemoveOutdatedSlaStatesFromCache() throws InterruptedException {
        throttlingService = buildThrottlingService(0, SLA_TTL);
        // First request uses guest SLA
        assertFalse(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
        assertTrue(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
        clock.ahead(SLA_TTL + 1);
        assertTrue(userService.isCleanupRequired());
        // Trigger cache clean
        assertFalse(throttlingService.isRequestAllowed(Optional.of(USER_B_TOKEN)));
        for(int i = 0; userService.isCleanupRequired() && i < 10; i++)
            Thread.sleep(1);
        assertFalse(userService.isCleanupRequired());
        // When SLA information removed from cache user request should use guest SLA
        assertFalse(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
    }

    @Test
    void isRequestAllowed_whenSlaExpiredTwice_shouldRemoveOutdatedSlaStatesFromCacheTwice() throws InterruptedException {
        throttlingService = buildThrottlingService(0, SLA_TTL);
        // First request uses guest SLA
        assertFalse(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
        assertFalse(throttlingService.isRequestAllowed(Optional.of(USER_B_TOKEN)));
        assertTrue(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
        for(int i = 0; i < 2; i++) {
            clock.ahead(SLA_TTL/2 + 1);
            assertTrue(throttlingService.isRequestAllowed(Optional.of(USER_B_TOKEN)));
            clock.ahead(SLA_TTL/2 + 1);
            assertTrue(userService.isCleanupRequired());
            // Trigger cache clean
            assertTrue(throttlingService.isRequestAllowed(Optional.of(USER_B_TOKEN)));
            for (int j = 0; userService.isCleanupRequired() && j < 1000; j++)
                Thread.sleep(1);
            assertFalse(userService.isCleanupRequired());
            // When SLA information removed from cache user request should use guest SLA
            assertFalse(throttlingService.isRequestAllowed(Optional.of(USER_A_TOKEN)));
        }
    }
}