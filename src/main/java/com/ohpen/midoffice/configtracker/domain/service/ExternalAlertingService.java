package com.ohpen.midoffice.configtracker.domain.service;

import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExternalAlertingService {

    @CircuitBreaker(name = "alertingService")
    @Retry(name = "alertingService")
    public void sendCriticalAlert(ConfigChange change) {
        log.warn("[ALERT] Sending critical change alert to external monitoring system: {}", change.id());
        // In a real scenario, this would call PagerDuty, OpsGenie, or a Security SOC
    }

    public void fallbackAlert(ConfigChange change, Throwable t) {
        log.error("[FALLBACK] Critical alert failed for change {}. Reason: {}", change.id(), t.getMessage());
        // Logic for emergency fallback (e.g., writing to a specific 'unnotified_critical_changes' log or table)
    }
}
