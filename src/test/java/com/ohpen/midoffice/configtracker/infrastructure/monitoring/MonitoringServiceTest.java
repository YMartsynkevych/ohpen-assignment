package com.ohpen.midoffice.configtracker.infrastructure.monitoring;

import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import com.ohpen.midoffice.configtracker.domain.service.ExternalAlertingService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private ExternalAlertingService alertingService;

    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        monitoringService = new MonitoringService(meterRegistry, alertingService);
        when(meterRegistry.counter(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(counter);
    }

    @Test
    void shouldIncrementCountersAndTriggerAlertForCriticalCreditLimit() {
        // Given
        Rule.CreditLimitRule rule = new Rule.CreditLimitRule(new BigDecimal("150000"), "USD", "CUST-1");
        ConfigChange change = new ConfigChange.AddedRule(UUID.randomUUID(), LocalDateTime.now(), "actor", "tenant", RuleType.CREDIT_LIMIT, rule);

        // When
        monitoringService.notifyIfCritical(change);

        // Then
        verify(meterRegistry).counter("config_tracker_changes_total", "operation", "ADD", "rule_type", "CREDIT_LIMIT");
        verify(meterRegistry).counter("config_tracker_critical_changes_total", "operation", "ADD", "rule_type", "CREDIT_LIMIT");
        verify(counter, times(2)).increment();
        verify(alertingService).sendCriticalAlert(change);
    }

    @Test
    void shouldOnlyIncrementGeneralCounterForNonCriticalChange() {
        // Given
        Rule.CreditLimitRule rule = new Rule.CreditLimitRule(new BigDecimal("50000"), "USD", "CUST-1");
        ConfigChange change = new ConfigChange.AddedRule(UUID.randomUUID(), LocalDateTime.now(), "actor", "tenant", RuleType.CREDIT_LIMIT, rule);

        // When
        monitoringService.notifyIfCritical(change);

        // Then
        verify(meterRegistry).counter("config_tracker_changes_total", "operation", "ADD", "rule_type", "CREDIT_LIMIT");
        verify(meterRegistry, never()).counter(eq("config_tracker_critical_changes_total"), anyString(), anyString(), anyString(), anyString());
        verify(counter, times(1)).increment();
        verify(alertingService, never()).sendCriticalAlert(any());
    }

    @Test
    void shouldIncrementCriticalCounterAndTriggerAlertOnDelete() {
        // Given
        Rule.CreditLimitRule rule = new Rule.CreditLimitRule(new BigDecimal("1000"), "USD", "CUST-1");
        ConfigChange change = new ConfigChange.RemovedRule(UUID.randomUUID(), LocalDateTime.now(), "actor", "tenant", RuleType.CREDIT_LIMIT, rule);

        // When
        monitoringService.notifyIfCritical(change);

        // Then
        verify(meterRegistry).counter("config_tracker_changes_total", "operation", "DELETE", "rule_type", "CREDIT_LIMIT");
        verify(meterRegistry).counter("config_tracker_critical_changes_total", "operation", "DELETE", "rule_type", "CREDIT_LIMIT");
        verify(counter, times(2)).increment();
        verify(alertingService).sendCriticalAlert(change);
    }

    @Test
    void shouldIncrementCriticalCounterAndTriggerAlertOnApproverCountChange() {
        // Given
        Rule.ApprovalPolicyRule oldRule = new Rule.ApprovalPolicyRule("Policy", List.of("A", "B"), 2);
        Rule.ApprovalPolicyRule newRule = new Rule.ApprovalPolicyRule("Policy", List.of("A", "B", "C"), 2);
        ConfigChange change = new ConfigChange.UpdatedRule(UUID.randomUUID(), LocalDateTime.now(), "actor", "tenant", RuleType.APPROVAL_POLICY, oldRule, newRule);

        // When
        monitoringService.notifyIfCritical(change);

        // Then
        verify(meterRegistry).counter("config_tracker_changes_total", "operation", "UPDATE", "rule_type", "APPROVAL_POLICY");
        verify(meterRegistry).counter("config_tracker_critical_changes_total", "operation", "UPDATE", "rule_type", "APPROVAL_POLICY");
        verify(counter, times(2)).increment();
        verify(alertingService).sendCriticalAlert(change);
    }
}
