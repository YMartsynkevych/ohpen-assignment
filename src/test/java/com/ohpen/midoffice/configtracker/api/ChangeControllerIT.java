package com.ohpen.midoffice.configtracker.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohpen.midoffice.configtracker.api.dto.ChangeRequest;
import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import com.ohpen.midoffice.configtracker.infrastructure.tenant.HeaderTenantProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChangeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndRetrieveChange() throws Exception {
        Rule payload = new Rule.CreditLimitRule(new BigDecimal("5000.00"), "USD", "CUST-999");
        ChangeRequest request = new ChangeRequest(
            RuleType.CREDIT_LIMIT,
            ChangeOperation.ADD,
            "tester",
            payload,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actor").value("tester"))
                .andExpect(jsonPath("$.type").value("CREDIT_LIMIT"))
                .andExpect(jsonPath("$.payload.amount").value(5000.00));
    }

    @Test
    void shouldFailWhenAddingExistingRule() throws Exception {
        Rule payload = new Rule.CreditLimitRule(new BigDecimal("1000.00"), "USD", "CUST-1");
        ChangeRequest request = new ChangeRequest(
            RuleType.CREDIT_LIMIT,
            ChangeOperation.ADD,
            "tester",
            payload,
            null,
            null
        );

        // First ADD should succeed
        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Second ADD should fail
        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldEnforceTenantIsolation() throws Exception {
        Rule payload = new Rule.CreditLimitRule(new BigDecimal("1000.00"), "USD", "SHARED-KEY");
        ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.ADD, "tester", payload, null, null);

        // Add for Tenant 1
        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify Tenant 1 sees it
        mockMvc.perform(get("/api/v1/changes?type=CREDIT_LIMIT")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Verify Tenant 2 DOES NOT see it
        mockMvc.perform(get("/api/v1/changes?type=CREDIT_LIMIT")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldFailWhenDeletingNonExistentRule() throws Exception {
        Rule payload = new Rule.CreditLimitRule(new BigDecimal("1000.00"), "USD", "NON-EXISTENT");
        ChangeRequest request = new ChangeRequest(
            RuleType.CREDIT_LIMIT,
            ChangeOperation.DELETE,
            "tester",
            payload,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
