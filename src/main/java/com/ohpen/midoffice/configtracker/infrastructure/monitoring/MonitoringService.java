package com.ohpen.midoffice.configtracker.infrastructure.monitoring;

import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
import com.ohpen.midoffice.configtracker.domain.service.ExternalAlertingService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Service
@Slf4j
public class MonitoringService {

    private static final BigDecimal CRITICAL_CREDIT_LIMIT = new BigDecimal("100000");

    private final MeterRegistry meterRegistry;
    private final ExternalAlertingService alertingService;

    public MonitoringService(MeterRegistry meterRegistry, ExternalAlertingService alertingService) {
        this.meterRegistry = meterRegistry;
        this.alertingService = alertingService;
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyIfCritical(ConfigChange change) {
        ChangeOperation op = getOperation(change);
        meterRegistry.counter("config_tracker_changes_total", 
            "operation", op.name(),
            "rule_type", change.ruleType().name()).increment();

        if (isCritical(change)) {
            meterRegistry.counter("config_tracker_critical_changes_total",
                "operation", op.name(),
                "rule_type", change.ruleType().name()).increment();
            log.warn("[CRITICAL CHANGE] Triggering alerting flow for: {}", change);
            alertingService.sendCriticalAlert(change);
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
        return switch (change) {
            case ConfigChange.AddedRule added -> isCriticalRule(added.newRule());
            case ConfigChange.UpdatedRule updated -> isCriticalRule(updated.newRule()) || isCriticalUpdate(updated);
            case ConfigChange.RemovedRule removed -> true; // Any deletion is critical
        };
    }

    private boolean isCriticalRule(Rule rule) {
        if (rule instanceof Rule.CreditLimitRule creditLimit) {
            return creditLimit.amount().compareTo(CRITICAL_CREDIT_LIMIT) > 0;
        }
        return false;
    }

    private boolean isCriticalUpdate(ConfigChange.UpdatedRule updated) {
        Rule oldRule = updated.oldRule();
        Rule newRule = updated.newRule();

        if (oldRule instanceof Rule.ApprovalPolicyRule oldPolicy && 
            newRule instanceof Rule.ApprovalPolicyRule newPolicy) {
            return oldPolicy.approvers().size() != newPolicy.approvers().size();
        }
        return false;
    }
}
