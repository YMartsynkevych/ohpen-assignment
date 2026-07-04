package com.ohpen.midoffice.configtracker.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import com.ohpen.midoffice.configtracker.domain.model.ConfigChangeEvent;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
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
        entity.setFromDomain(change, objectMapper);
        changeRepository.save(entity);

        // 2. Update current state
        updateCurrentState(change);

        // 3. Notify and Publish
        eventPublisher.publishEvent(new ConfigChangeEvent(this, change));
        monitoringService.notifyIfCritical(change);

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
