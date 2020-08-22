package com.throttler.map;

import java.util.Collection;

public class CacheCleanerTask implements Runnable {
    UserService userService;
    TokenService tokenService;

    public CacheCleanerTask(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @Override
    public void run() {
        if(userService.hasOutdatedSlaStates()) {
            final Collection<String> tokensToRemove = userService.removeOldSlaStates();
            tokenService.removeTokens(tokensToRemove);
        }
    }
}
