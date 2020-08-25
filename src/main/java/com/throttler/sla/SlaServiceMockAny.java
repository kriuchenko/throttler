package com.throttler.sla;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service("slaServiceMockAny")
public class SlaServiceMockAny implements SlaService{
    private boolean async = false;
    private int default_rps;
    Map<String, SLA> slas;

    public SlaServiceMockAny(@Value("${mockSlaServiceAsync}") boolean async, @Value("${mockUserRps}") int default_rps) {
        this.async = async;
        this.default_rps = default_rps;
    }

    @Override
    public CompletableFuture<SLA> getSlaByToken(String token) {
        CompletableFuture<SLA> result = new CompletableFuture<SLA>();
        if(async)
            CompletableFuture.runAsync(() -> slas.get(token));
        else
            result.complete(slas.get(token));
        return result;
    }

    private SLA buildSla(String token){
        return new SLA("USER_" + token.toUpperCase(), default_rps);
    }
}
