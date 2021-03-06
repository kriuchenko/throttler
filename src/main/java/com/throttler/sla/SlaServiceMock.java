package com.throttler.sla;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service("slaServiceMock")
public class SlaServiceMock implements SlaService{
    private static final Map<String, SLA> DEFAULT_SLAS = Map.of(
            "a",new SLA("a", 1),
            "b",new SLA("B", 3)
            );
    private boolean async = false;
    Map<String, SLA> slas;

    public SlaServiceMock() {
        this.slas = DEFAULT_SLAS;
    }
    public SlaServiceMock(boolean async) {
        this.async = async;
        this.slas = DEFAULT_SLAS;
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
}
