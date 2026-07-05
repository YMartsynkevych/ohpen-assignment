package com.ohpen.midoffice.configtracker.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohpen.midoffice.configtracker.api.dto.ChangeRequest;
import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import com.ohpen.midoffice.configtracker.infrastructure.persistence.ConfigChangeEntity;
import com.ohpen.midoffice.configtracker.infrastructure.persistence.ConfigChangeRepository;
import com.ohpen.midoffice.configtracker.infrastructure.persistence.RuleStateEntity;
import com.ohpen.midoffice.configtracker.infrastructure.persistence.RuleStateRepository;
import com.ohpen.midoffice.configtracker.infrastructure.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigChangeService {

    private final ConfigChangeRepository changeRepository;
    private final RuleStateRepository ruleStateRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ConfigChange processChangeRequest(ChangeRequest request) {
        log.info("Processing change request: {}", request);
        
        ConfigChange change = switch (request.operation()) {
            case ADD -> {
                validateAdd(request);
                yield new ConfigChange.AddedRule(
                    UUID.randomUUID(), LocalDateTime.now(), request.actor(), TenantContext.getTenantId(), request.type(), request.payload()
                );
            }
            case UPDATE -> {
                validateUpdate(request);
                yield new ConfigChange.UpdatedRule(
                    UUID.randomUUID(), LocalDateTime.now(), request.actor(), TenantContext.getTenantId(), request.type(), request.oldPayload(), request.newPayload()
                );
            }
            case DELETE -> {
                validateDelete(request);
                yield new ConfigChange.RemovedRule(
                    UUID.randomUUID(), LocalDateTime.now(), request.actor(), TenantContext.getTenantId(), request.type(), request.payload()
                );
            }
        };

        return trackChange(change);
    }

    private void validateAdd(ChangeRequest request) {
        String key = extractKey(request.payload());
        if (ruleStateRepository.findByRuleTypeAndRuleKey(request.type(), key).isPresent()) {
            throw new IllegalStateException("Cannot add rule: Rule already exists for key " + key);
        }
    }

    private void validateUpdate(ChangeRequest request) {
        String key = extractKey(request.newPayload());
        Optional<RuleStateEntity> existing = ruleStateRepository.findByRuleTypeAndRuleKey(request.type(), key);
        if (existing.isEmpty()) {
            throw new IllegalStateException("Cannot update rule: Rule does not exist for key " + key);
        }
        // Additional validation: ensure oldPayload matches existing state
        try {
            String existingPayloadJson = existing.get().getPayloadJson();
            String oldPayloadJson = objectMapper.writeValueAsString(request.oldPayload());
            if (!existingPayloadJson.equals(oldPayloadJson)) {
                 log.warn("Update payload mismatch for key {}. Existing: {}, Provided: {}", key, existingPayloadJson, oldPayloadJson);
                 // In some strict systems we would throw here. 
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateDelete(ChangeRequest request) {
        String key = extractKey(request.payload());
        if (ruleStateRepository.findByRuleTypeAndRuleKey(request.type(), key).isEmpty()) {
            throw new IllegalStateException("Cannot delete rule: Rule does not exist for key " + key);
        }
    }

    @Transactional
    public ConfigChange trackChange(ConfigChange change) {
        log.info("Tracking change: {}", change);
        
        // 1. Save to history
        ConfigChangeEntity entity = new ConfigChangeEntity();
        entity.setFromDomain(change, objectMapper);
        changeRepository.save(entity);

        // 2. Update current state
        updateCurrentState(change);

        // 3. Notify (only after successful commit)
        eventPublisher.publishEvent(change);

        return change;
    }

    private void updateCurrentState(ConfigChange change) {
        Rule payload = switch (change) {
            case ConfigChange.AddedRule added -> added.newRule();
            case ConfigChange.UpdatedRule updated -> updated.newRule();
            case ConfigChange.RemovedRule removed -> removed.removedRule();
        };

        String key = extractKey(payload);
        Optional<RuleStateEntity> existingState = ruleStateRepository.findByRuleTypeAndRuleKey(change.ruleType(), key);

        if (change instanceof ConfigChange.RemovedRule) {
            existingState.ifPresent(ruleStateRepository::delete);
        } else {
            RuleStateEntity state = existingState.orElse(new RuleStateEntity());
            state.setTenantId(change.tenantId());
            state.setRuleType(change.ruleType());
            state.setRuleKey(key);
            try {
                state.setPayloadJson(objectMapper.writeValueAsString(payload));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            state.setLastModified(LocalDateTime.now());
            ruleStateRepository.save(state);
        }
    }

    private String extractKey(Rule payload) {
        return switch (payload) {
            case Rule.CreditLimitRule cl -> cl.customerId();
            case Rule.ApprovalPolicyRule ap -> ap.policyName();
        };
    }

    public Optional<ConfigChange> getChangeById(UUID id) {
        return changeRepository.findById(id).map(this::toDomain);
    }

    public List<ConfigChange> getChangesByType(RuleType type) {
        return changeRepository.findByRuleType(type).stream().map(this::toDomain).toList();
    }

    public List<ConfigChange> getChangesByTimeRange(LocalDateTime start, LocalDateTime end) {
        return changeRepository.findByTimestampBetween(start, end).stream().map(this::toDomain).toList();
    }

    private ConfigChange toDomain(ConfigChangeEntity entity) {
        return entity.toDomain(objectMapper);
    }
}
