package com.ohpen.midoffice.configtracker.infrastructure.monitoring;

import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MonitoringService {
    
    public void notifyIfCritical(ConfigChange change) {
        if (isCritical(change)) {
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
