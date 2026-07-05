package com.ohpen.midoffice.configtracker.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohpen.midoffice.configtracker.api.dto.ChangeRequest;
import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import com.ohpen.midoffice.configtracker.infrastructure.tenant.HeaderTenantProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ohpen.midoffice.configtracker.infrastructure.persistence.ConfigChangeRepository;
import com.ohpen.midoffice.configtracker.infrastructure.persistence.RuleStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

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

    @Autowired
    private ConfigChangeRepository configChangeRepository;

    @Autowired
    private RuleStateRepository ruleStateRepository;

    @BeforeEach
    void setUp() {
        ruleStateRepository.deleteAll();
        configChangeRepository.deleteAll();
    }

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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Cannot delete rule: Rule does not exist for key NON-EXISTENT"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn404WhenChangeNotFound() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/changes/" + randomId)
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Change not found with id: " + randomId))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn400WhenValidationFails() throws Exception {
        ChangeRequest request = new ChangeRequest(
            null, // missing type
            ChangeOperation.ADD,
            "",   // blank actor
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldFailWhenUpdatingWithStaleState() throws Exception {
        String customerId = "CUST-LOCK";
        Rule initialRule = new Rule.CreditLimitRule(new BigDecimal("1000.00"), "USD", customerId);
        
        // 1. Initial ADD
        ChangeRequest addRequest = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.ADD, "tester", initialRule, null, null);
        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk());

        // 2. First update (Success)
        Rule updatedRule1 = new Rule.CreditLimitRule(new BigDecimal("2000.00"), "USD", customerId);
        ChangeRequest updateRequest1 = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.UPDATE, "tester", null, initialRule, updatedRule1);
        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest1)))
                .andExpect(status().isOk());

        // 3. Second update using STALE initialRule as oldPayload (Failure)
        Rule updatedRule2 = new Rule.CreditLimitRule(new BigDecimal("3000.00"), "USD", customerId);
        ChangeRequest updateRequest2 = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.UPDATE, "tester", null, initialRule, updatedRule2);
        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Update payload mismatch: Provided old state does not match current system state. This might be due to a concurrent update."));
    }

}
