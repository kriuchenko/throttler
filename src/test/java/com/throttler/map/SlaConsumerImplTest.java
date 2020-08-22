package com.throttler.map;

import com.throttler.clock.FrozenClock;
import com.throttler.sla.SlaService;
import com.throttler.sla.SlaServiceMock;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlaConsumerImplTest {
    private static final int GUEST_RPS = 0;
    private static final int SLA_TTL = 0;
    FrozenClock clock = new FrozenClock(0);
    private final SlaService slaService = new SlaServiceMock();
    private final UserService userService = new UserService(GUEST_RPS, SLA_TTL, clock);
    private final TokenService tokenService = new TokenService(slaService);
    private final SlaConsumer dummySlaConsumer = (token, sla) -> {
    };

    @Test
    void accept_shouldRegisterSlaAndCreateToken() {
        String token = "a";
        String user = "A";
        int rps = 123;
        SlaConsumerImpl slaConsumer = new SlaConsumerImpl(userService, tokenService);
        SlaService.SLA sla = new SlaService.SLA(user, rps);
        slaConsumer.accept(token, sla);
        final Optional<SlaState> slaState = userService.getSlaState(user);
        assertTrue(slaState.isPresent());
        assertEquals(rps, slaState.get().getRps());

        final Optional<String> resolvedUser = tokenService.resolveToken(token, dummySlaConsumer);
        assertTrue(resolvedUser.isPresent());
        assertEquals(user, resolvedUser.get());
    }
}