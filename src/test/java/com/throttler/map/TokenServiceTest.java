package com.throttler.map;

import com.throttler.sla.SlaService;
import com.throttler.sla.SlaServiceMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenServiceTest {
    private static final String TOKEN = "a";
    private static final String USER = "A";
    private SlaConsumer dummySlaConsumer;
    private final SlaService slaService = spy(new SlaServiceMock());
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(slaService);
        dummySlaConsumer = spy(new SlaConsumer() {
            @Override
            public void accept(String token, SlaService.SLA sla) {

            }
        });
    }

    @Test
    void resolveToken_givenResolvedToken_shouldReturnUser() {
        tokenService.register(TOKEN, USER);
        assertEquals(Optional.of(USER), tokenService.resolveToken(TOKEN, dummySlaConsumer));
        verify(slaService, never()).getSlaByToken(any());
    }

    @Test
    void resolveToken_givenUnresolvedToken_whenResolutionAlreadyInitiated_shouldReturnNoneAndNotInitiateResolution() {
        tokenService.lockToken(TOKEN);
        assertEquals(Optional.empty(), tokenService.resolveToken(TOKEN, dummySlaConsumer));
        verify(slaService, never()).getSlaByToken(any());
    }

    @Test
    void resolveToken_givenUnresolvedToken_whenResolutionNotInitiated_shouldReturnNoneAndInitiateResolution() {
        assertEquals(Optional.empty(), tokenService.resolveToken(TOKEN, dummySlaConsumer));
        verify(slaService).getSlaByToken(TOKEN);
        verify(dummySlaConsumer).accept(any(), any());
        assertTrue("Token should be unlocked after resolution", tokenService.lockToken(TOKEN));
    }

    @Test
    void removeTokens() {
        tokenService.register("a", "A");
        tokenService.register("b", "B");
        tokenService.register("c", "C");
        tokenService.removeTokens(Set.of("a", "b"));
        assertEquals(Optional.empty(), tokenService.resolveToken("a", dummySlaConsumer));
        assertEquals(Optional.empty(), tokenService.resolveToken("b", dummySlaConsumer));
        assertEquals(Optional.of("C"), tokenService.resolveToken("c", dummySlaConsumer));
    }
}