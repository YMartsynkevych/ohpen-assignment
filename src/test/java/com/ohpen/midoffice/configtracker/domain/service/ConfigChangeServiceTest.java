package com.ohpen.midoffice.configtracker.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohpen.midoffice.configtracker.api.dto.ChangeRequest;
import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import com.ohpen.midoffice.configtracker.infrastructure.persistence.ConfigChangeEntity;
import com.ohpen.midoffice.configtracker.infrastructure.persistence.ConfigChangeRepository;
import com.ohpen.midoffice.configtracker.infrastructure.persistence.RuleStateEntity;
import com.ohpen.midoffice.configtracker.infrastructure.persistence.RuleStateRepository;
import com.ohpen.midoffice.configtracker.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigChangeServiceTest {

    @Mock
    private ConfigChangeRepository changeRepository;

    @Mock
    private RuleStateRepository ruleStateRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ExternalNotificationService notificationService;

    @InjectMocks
    private ConfigChangeService configChangeService;

    private final String tenantId = "tenant-1";
    private final String actor = "test-actor";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldProcessAddRequest() throws JsonProcessingException {
        // Given
        Rule.CreditLimitRule payload = new Rule.CreditLimitRule(new BigDecimal("1000"), "USD", "CUST-1");
        ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.ADD, actor, payload, null, null);

        when(ruleStateRepository.findByRuleTypeAndRuleKey(RuleType.CREDIT_LIMIT, "CUST-1")).thenReturn(Optional.empty());

        // When
        ConfigChange result = configChangeService.processChangeRequest(request);

        // Then
        assertThat(result).isInstanceOf(ConfigChange.AddedRule.class);
        assertThat(result.actor()).isEqualTo(actor);
        assertThat(result.tenantId()).isEqualTo(tenantId);
        
        verify(changeRepository).save(any(ConfigChangeEntity.class));
        verify(ruleStateRepository).save(any(RuleStateEntity.class));
        verify(eventPublisher).publishEvent(result);
        verify(notificationService).notifyExternalSystem(result);
    }

    @Test
    void shouldFailAddWhenRuleExists() {
        // Given
        Rule.CreditLimitRule payload = new Rule.CreditLimitRule(new BigDecimal("1000"), "USD", "CUST-1");
        ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.ADD, actor, payload, null, null);

        when(ruleStateRepository.findByRuleTypeAndRuleKey(RuleType.CREDIT_LIMIT, "CUST-1"))
                .thenReturn(Optional.of(new RuleStateEntity()));

        // When / Then
        assertThatThrownBy(() -> configChangeService.processChangeRequest(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldProcessUpdateRequest() throws JsonProcessingException {
        // Given
        Rule.CreditLimitRule oldPayload = new Rule.CreditLimitRule(new BigDecimal("1000"), "USD", "CUST-1");
        Rule.CreditLimitRule newPayload = new Rule.CreditLimitRule(new BigDecimal("2000"), "USD", "CUST-1");
        ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.UPDATE, actor, null, oldPayload, newPayload);

        RuleStateEntity existingEntity = new RuleStateEntity();
        existingEntity.setPayloadJson(objectMapper.writeValueAsString(oldPayload));

        when(ruleStateRepository.findByRuleTypeAndRuleKey(RuleType.CREDIT_LIMIT, "CUST-1")).thenReturn(Optional.of(existingEntity));

        // When
        ConfigChange result = configChangeService.processChangeRequest(request);

        // Then
        assertThat(result).isInstanceOf(ConfigChange.UpdatedRule.class);
        verify(ruleStateRepository).save(any(RuleStateEntity.class));
    }

    @Test
    void shouldFailUpdateWhenRuleDoesNotExist() {
        // Given
        Rule.CreditLimitRule oldPayload = new Rule.CreditLimitRule(new BigDecimal("1000"), "USD", "CUST-1");
        Rule.CreditLimitRule newPayload = new Rule.CreditLimitRule(new BigDecimal("2000"), "USD", "CUST-1");
        ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.UPDATE, actor, null, oldPayload, newPayload);

        when(ruleStateRepository.findByRuleTypeAndRuleKey(RuleType.CREDIT_LIMIT, "CUST-1")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> configChangeService.processChangeRequest(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void shouldFailUpdateWhenOldPayloadMismatch() throws JsonProcessingException {
        // Given
        Rule.CreditLimitRule actualStoredPayload = new Rule.CreditLimitRule(new BigDecimal("500"), "USD", "CUST-1");
        Rule.CreditLimitRule providedOldPayload = new Rule.CreditLimitRule(new BigDecimal("1000"), "USD", "CUST-1");
        Rule.CreditLimitRule newPayload = new Rule.CreditLimitRule(new BigDecimal("2000"), "USD", "CUST-1");
        ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.UPDATE, actor, null, providedOldPayload, newPayload);

        RuleStateEntity existingEntity = new RuleStateEntity();
        existingEntity.setPayloadJson(objectMapper.writeValueAsString(actualStoredPayload));

        when(ruleStateRepository.findByRuleTypeAndRuleKey(RuleType.CREDIT_LIMIT, "CUST-1")).thenReturn(Optional.of(existingEntity));

        // When / Then
        assertThatThrownBy(() -> configChangeService.processChangeRequest(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("payload mismatch");
    }

    @Test
    void shouldProcessDeleteRequest() {
        // Given
        Rule.CreditLimitRule payload = new Rule.CreditLimitRule(new BigDecimal("1000"), "USD", "CUST-1");
        ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.DELETE, actor, payload, null, null);

        RuleStateEntity existingEntity = new RuleStateEntity();
        when(ruleStateRepository.findByRuleTypeAndRuleKey(RuleType.CREDIT_LIMIT, "CUST-1")).thenReturn(Optional.of(existingEntity));

        // When
        ConfigChange result = configChangeService.processChangeRequest(request);

        // Then
        assertThat(result).isInstanceOf(ConfigChange.RemovedRule.class);
        verify(ruleStateRepository).delete(existingEntity);
    }

    @Test
    void shouldFailDeleteWhenRuleDoesNotExist() {
        // Given
        Rule.CreditLimitRule payload = new Rule.CreditLimitRule(new BigDecimal("1000"), "USD", "CUST-1");
        ChangeRequest request = new ChangeRequest(RuleType.CREDIT_LIMIT, ChangeOperation.DELETE, actor, payload, null, null);

        when(ruleStateRepository.findByRuleTypeAndRuleKey(RuleType.CREDIT_LIMIT, "CUST-1")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> configChangeService.processChangeRequest(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not exist");
    }
}
