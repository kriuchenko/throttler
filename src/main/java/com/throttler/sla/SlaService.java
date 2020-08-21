package com.throttler.sla;

import java.util.concurrent.CompletableFuture;
public interface SlaService {
    CompletableFuture<SLA> getSlaByToken(String token);
    public static class SLA {
        private final String user;
        private final int rps;
        public SLA(String user, int rps) {
            super();
            this.user = user;
            this.rps = rps;
        }
        public String getUser() {
            return this.user;
        }
        public int getRps() {
            return this.rps;
        }
    }
}