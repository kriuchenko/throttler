package com.throttler.map;

import com.throttler.ThrottlingService;
import com.throttler.clock.Clock;
import com.throttler.clock.SystemClock;
import com.throttler.sla.SlaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/*
    TODO Add comments
 */
@Service("mapBasedThrottlingService")
public class MapBasedThrottlingService implements ThrottlingService {
    private final int slaTTl;
    private final TokenService tokenService;
    private final UserService userService;
    private final SlaStateService slaStateService;
    private final SlaConsumer slaConsumer;
    private final Clock clock;
    private final AtomicBoolean cleaning = new AtomicBoolean();
    private final CacheCleanerTask cacheCleanerTask;

    /*
        @param  guestRps    RPS for unauthorized users
        @param  cacheTtl    Time to keep access information in cache in milliseconds (0 for infinite).
        @param  SlaService  Sla service
     */
    @Autowired
    public MapBasedThrottlingService(@Value("${guestRps}") int guestRps,
                                     @Value("${slaTTl}") int slaTTl,
                                     @Qualifier("slaServiceMockAny") SlaService slaService) {
        this.slaTTl = slaTTl;
        this.clock = new SystemClock();
        this.tokenService = new TokenService(slaService);
        this.userService = new UserService(guestRps, slaTTl, clock);
        this.slaStateService = new SlaStateService(clock);
        this.slaConsumer = new SlaConsumerImpl(userService, tokenService);
        this.cacheCleanerTask = new CacheCleanerTask(userService, tokenService);
    }

    public MapBasedThrottlingService(int slaTtl, TokenService tokenService, UserService userService, SlaStateService slaStateService, Clock clock) {
        this.slaTTl = slaTtl;
        this.tokenService = tokenService;
        this.userService = userService;
        this.slaStateService = slaStateService;
        this.clock = clock;
        this.slaConsumer = new SlaConsumerImpl(userService, tokenService);
        this.cacheCleanerTask = new CacheCleanerTask(userService, tokenService);
    }

    @Override
    public boolean isRequestAllowed(Optional<String> maybeToken){
        Optional<String> maybeUser = maybeToken.flatMap(token ->
            tokenService.resolveToken(token, slaConsumer)
        );
        SlaState slaState = maybeUser.flatMap(userService::getSlaState).orElse(userService.getGuestSlaState());
        maybeToken.ifPresent(token -> refreshToken(token, slaState.getValidAt()));
        final boolean accessAllowed = slaStateService.tryAccess(slaState);
        cleanCache();
        return accessAllowed;
    }

    private void refreshToken(String token, long validAt){
        if(clock.getMillis() - validAt > slaTTl)
            tokenService.fetchSla(token, slaConsumer);
    }

    private void cleanCache(){
        if(userService.isCleanupRequired() && !cleaning.getAndSet(true))
            CompletableFuture.runAsync(cacheCleanerTask).whenComplete((v, t) -> cleaning.set(false));
    }
}