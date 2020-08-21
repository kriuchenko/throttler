package com.throttler.map;

import com.throttler.sla.SlaService;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TokenService {
    private final Set<String> tokensInProgress = ConcurrentHashMap.newKeySet();
    private final Map<String, String> tokenUsers = new ConcurrentHashMap<>();
    private final SlaService slaService;

    public TokenService(SlaService slaService) {
        this.slaService = slaService;
    }

    public boolean lockToken(String token){
        return tokensInProgress.add(token);
    }

    public boolean unlockToken(String token){
        return tokensInProgress.remove(token);
    }

    public Optional<String> resolveToken(String token, SlaConsumer slaConsumer) {
        String user = tokenUsers.get(token);
        if (user != null)
            return Optional.of(user);
        else {
            fetchSla(token, slaConsumer);
            return Optional.empty();
        }
    }

    public void fetchSla(String token, SlaConsumer slaConsumer) {
        boolean tokenLocked = lockToken(token);
        if(tokenLocked)
            slaService.getSlaByToken(token)
                .thenAccept(sla -> slaConsumer.accept(token, sla))
                .whenComplete((sla, t) -> unlockToken(token));
    }

    public void register(String token, String user) {
        tokenUsers.put(token, user);
    }

    public void removeTokens(Collection<String> tokens) {
        tokenUsers.keySet().removeAll(tokens);
    }
}
