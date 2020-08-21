package com.throttler.map;

import java.util.Collection;

public class CacheCleanerTask implements Runnable {
    long cacheTtl;
    UserService userService;
    TokenService tokenService;

    public CacheCleanerTask(long cacheTtl, UserService userService, TokenService tokenService) {
        this.cacheTtl = cacheTtl;
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @Override
    public void run() {
        if(userService.hasOutdatedSlaStates(cacheTtl)) {
            final Collection<String> tokensToRemove = userService.removeOldSlaStates(cacheTtl);
            tokenService.removeTokens(tokensToRemove);
        }
    }
}
