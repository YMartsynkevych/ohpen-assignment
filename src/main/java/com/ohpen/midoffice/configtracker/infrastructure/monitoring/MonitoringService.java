package com.ohpen.midoffice.configtracker.infrastructure.monitoring;

import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MonitoringService {

    private final MeterRegistry meterRegistry;

    public MonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void notifyIfCritical(ConfigChange change) {
        meterRegistry.counter("config_tracker_changes_total", 
            "operation", change.operation().name(),
            "rule_type", change.ruleType().name()).increment();

        if (isCritical(change)) {
            meterRegistry.counter("config_tracker_critical_changes_total",
                "operation", change.operation().name(),
                "rule_type", change.ruleType().name()).increment();
            log.warn("[CRITICAL CHANGE] Notification sent to external monitoring for: {}", change);
        }
    }

    private boolean isCritical(ConfigChange change) {
        // Logic for determining criticality
        // For example, any DELETE or high-value CREDIT_LIMIT change
        return switch (change.operation()) {
            case DELETE -> true;
            case UPDATE, ADD -> false; // Could be more complex
        };
    }
}
