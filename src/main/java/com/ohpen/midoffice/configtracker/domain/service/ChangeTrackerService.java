package com.ohpen.midoffice.configtracker.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import com.ohpen.midoffice.configtracker.domain.model.ConfigChangeEvent;
import com.ohpen.midoffice.configtracker.domain.model.RulePayload;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import com.ohpen.midoffice.configtracker.infrastructure.persistence.*;
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
public class ChangeTrackerService {

    private final ConfigChangeRepository changeRepository;
    private final RuleStateRepository ruleStateRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final com.ohpen.midoffice.configtracker.infrastructure.monitoring.MonitoringService monitoringService;

    @Transactional
    public ConfigChange trackChange(ConfigChange change) {
        log.info("Tracking change: {}", change);
        
        // 1. Save to history
        ConfigChangeEntity entity = new ConfigChangeEntity();
        entity.setId(change.id());
        entity.setRuleType(change.ruleType());
        entity.setOperation(change.operation());
        entity.setPayload(change.payload(), objectMapper);
        entity.setTimestamp(change.timestamp());
        entity.setActor(change.actor());
        changeRepository.save(entity);

        // 2. Update current state
        updateCurrentState(change);

        // 3. Notify and Publish
        eventPublisher.publishEvent(new ConfigChangeEvent(this, change));
        monitoringService.notifyIfCritical(change);

        return change;
    }

    private void updateCurrentState(ConfigChange change) {
        String key = extractKey(change.payload());
        Optional<RuleStateEntity> existingState = ruleStateRepository.findByRuleTypeAndRuleKey(change.ruleType(), key);

        switch (change.operation()) {
            case ADD, UPDATE -> {
                RuleStateEntity state = existingState.orElse(new RuleStateEntity());
                state.setRuleType(change.ruleType());
                state.setRuleKey(key);
                try {
                    state.setPayloadJson(objectMapper.writeValueAsString(change.payload()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                state.setLastModified(LocalDateTime.now());
                ruleStateRepository.save(state);
            }
            case DELETE -> existingState.ifPresent(ruleStateRepository::delete);
        }
    }

    private String extractKey(RulePayload payload) {
        return switch (payload) {
            case RulePayload.CreditLimitPayload cl -> cl.customerId();
            case RulePayload.ApprovalPolicyPayload ap -> ap.policyName();
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
        return new ConfigChange(
            entity.getId(),
            entity.getRuleType(),
            entity.getOperation(),
            entity.getPayload(objectMapper),
            entity.getTimestamp(),
            entity.getActor(),
            null // metadata not implemented in entity yet
        );
    }
}
