package com.throttler.map;

import com.throttler.sla.SlaService;

public interface SlaConsumer{
    public void accept(String token, SlaService.SLA sla);
}