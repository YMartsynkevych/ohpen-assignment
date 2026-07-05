package com.ohpen.midoffice.configtracker.api;

import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import com.ohpen.midoffice.configtracker.domain.service.ExternalNotificationService;
import com.ohpen.midoffice.configtracker.api.dto.ChangeRequest;
import com.ohpen.midoffice.configtracker.infrastructure.tenant.HeaderTenantProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ResiliencyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoSpyBean
    private ExternalNotificationService notificationService;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("externalService").reset();
    }

    @Test
    void shouldFailWhenNotificationFails() throws Exception {
        // Force the spy to throw an exception
        doThrow(new RuntimeException("Simulated failure"))
            .when(notificationService).notifyExternalSystem(any());

        Rule payload = new Rule.CreditLimitRule(new BigDecimal("1000.00"), "USD", "RES-1");
        ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.ADD, "tester", payload, null, null);

        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));

        verify(notificationService, atLeastOnce()).notifyExternalSystem(any());
    }

    @Test
    void shouldReturn503WhenCircuitBreakerIsOpen() throws Exception {
        // Force the spy to throw an exception to simulate failure
        doThrow(new RuntimeException("Simulated failure"))
            .when(notificationService).notifyExternalSystem(any());

        // Configuration in application.yaml:
        // minimumNumberOfCalls: 5
        // slidingWindowSize: 10
        // failureRateThreshold: 50

        // Send 5 failing requests with unique keys to trigger the circuit breaker
        for (int i = 0; i < 5; i++) {
            Rule payload = new Rule.CreditLimitRule(new BigDecimal("1000.00"), "USD", "CB-TEST-" + i);
            ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.ADD, "tester", payload, null, null);

            try {
                mockMvc.perform(post("/api/v1/changes")
                        .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
            } catch (Exception e) {
                // Expected internal server error due to Simulated failure
            }
        }

        // The 6th request should fail with 503 because the circuit breaker is now OPEN
        Rule payload = new Rule.CreditLimitRule(new BigDecimal("1000.00"), "USD", "CB-TEST-FINAL");
        ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.ADD, "tester", payload, null, null);

        mockMvc.perform(post("/api/v1/changes")
                .header(HeaderTenantProvider.TENANT_HEADER, "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"));
    }
}
