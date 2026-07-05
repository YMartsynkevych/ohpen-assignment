package com.ohpen.midoffice.configtracker.domain.service;

import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExternalNotificationService {

    @CircuitBreaker(name = "externalService")
    @Retry(name = "externalService")
    public void notifyExternalSystem(ConfigChange change) {
        log.info("Attempting to notify external system for change: {}", change.id());
        // In a real scenario, this would be a REST call or message queue publish
    }

    public void fallbackNotify(ConfigChange change, Throwable t) {
        log.error("Fallback triggered for change {}. Reason: {}", change.id(), t.getMessage());
        // Logic for fallback: e.g., save to a 'failed_notifications' table or send to DLQ
    }
}
