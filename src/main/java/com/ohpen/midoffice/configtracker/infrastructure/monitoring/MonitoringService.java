package com.ohpen.midoffice.configtracker.infrastructure.monitoring;

import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
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
        ChangeOperation op = getOperation(change);
        meterRegistry.counter("config_tracker_changes_total", 
            "operation", op.name(),
            "rule_type", change.ruleType().name()).increment();

        if (isCritical(change)) {
            meterRegistry.counter("config_tracker_critical_changes_total",
                "operation", op.name(),
                "rule_type", change.ruleType().name()).increment();
            log.warn("[CRITICAL CHANGE] Notification sent to external monitoring for: {}", change);
        }
    }

    private ChangeOperation getOperation(ConfigChange change) {
        return switch (change) {
            case ConfigChange.AddedRule a -> ChangeOperation.ADD;
            case ConfigChange.UpdatedRule u -> ChangeOperation.UPDATE;
            case ConfigChange.RemovedRule r -> ChangeOperation.DELETE;
        };
    }

    private boolean isCritical(ConfigChange change) {
        // Logic for determining criticality
        // For example, any DELETE or high-value CREDIT_LIMIT change
        return change instanceof ConfigChange.RemovedRule;
    }
}
