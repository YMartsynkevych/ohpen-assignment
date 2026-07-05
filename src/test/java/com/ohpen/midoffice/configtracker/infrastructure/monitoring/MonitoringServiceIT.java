package com.ohpen.midoffice.configtracker.infrastructure.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohpen.midoffice.configtracker.api.dto.ChangeRequest;
import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import com.ohpen.midoffice.configtracker.infrastructure.tenant.HeaderTenantProvider;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MonitoringServiceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void clearRegistry() {
        meterRegistry.clear();
    }

    @Test
    void shouldIncrementCriticalMetricForHighCreditLimit() throws Exception {
        double initialCritical = getCriticalCount();

        Rule payload = new Rule.CreditLimitRule(new BigDecimal("150000"), "USD", "HIGH-CUST");
        ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.ADD, "tester", payload, null, null);

        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-monitoring")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertEquals(initialCritical + 1, getCriticalCount(), "Critical metric should be incremented");
    }

    @Test
    void shouldIncrementCriticalMetricForApproverCountChange() throws Exception {
        // 1. Add initial policy
        Rule initialPayload = new Rule.ApprovalPolicyRule("POLICY-1", List.of("user1"), 1);
        ChangeRequest addRequest = new ChangeRequest(RuleType.APPROVAL_POLICY, ChangeOperation.ADD, "tester", initialPayload, null, null);
        
        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-monitoring")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk());

        double initialCritical = getCriticalCount();

        // 2. Update with different approver count
        Rule updatedPayload = new Rule.ApprovalPolicyRule("POLICY-1", List.of("user1", "user2"), 1);
        ChangeRequest updateRequest = new ChangeRequest(RuleType.APPROVAL_POLICY, ChangeOperation.UPDATE, "tester", null, initialPayload, updatedPayload);

        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-monitoring")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        assertEquals(initialCritical + 1, getCriticalCount(), "Critical metric should be incremented when approver count changes");
    }

    @Test
    void shouldNotIncrementCriticalMetricForNormalCreditLimit() throws Exception {
        double initialCritical = getCriticalCount();

        Rule payload = new Rule.CreditLimitRule(new BigDecimal("50000"), "USD", "NORMAL-CUST");
        ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.ADD, "tester", payload, null, null);

        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-monitoring")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertEquals(initialCritical, getCriticalCount(), "Critical metric should NOT be incremented for normal amount");
    }

    private double getCriticalCount() {
        try {
            return meterRegistry.get("config_tracker_critical_changes_total").counter().count();
        } catch (Exception e) {
            return 0.0;
        }
    }
}
