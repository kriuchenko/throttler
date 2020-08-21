package com.throttler.map;

import com.throttler.sla.SlaService;

public class SlaConsumerImpl implements SlaConsumer {
    private final UserService userService;
    private final TokenService tokenService;

    public SlaConsumerImpl(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @Override
    public void accept(String token, SlaService.SLA sla) {
        userService.setSlaState(token, sla);
        tokenService.register(token, sla.getUser());
    }
}