package com.ohpen.midoffice.configtracker.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "config_changes")
@Data
@NoArgsConstructor
public class ConfigChangeEntity {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    private ChangeOperation operation;

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    @Column(columnDefinition = "TEXT")
    private String oldPayloadJson;

    private LocalDateTime timestamp;
    private String actor;

    public void setFromDomain(ConfigChange change, ObjectMapper objectMapper) {
        this.id = change.id();
        this.ruleType = change.ruleType();
        this.timestamp = change.timestamp();
        this.actor = change.actor();
        try {
            switch (change) {
                case ConfigChange.AddedRule added -> {
                    this.operation = ChangeOperation.ADD;
                    this.payloadJson = objectMapper.writeValueAsString(added.newRule());
                }
                case ConfigChange.UpdatedRule updated -> {
                    this.operation = ChangeOperation.UPDATE;
                    this.oldPayloadJson = objectMapper.writeValueAsString(updated.oldRule());
                    this.payloadJson = objectMapper.writeValueAsString(updated.newRule());
                }
                case ConfigChange.RemovedRule removed -> {
                    this.operation = ChangeOperation.DELETE;
                    this.payloadJson = objectMapper.writeValueAsString(removed.removedRule());
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing domain object", e);
        }
    }

    public ConfigChange toDomain(ObjectMapper objectMapper) {
        try {
            return switch (this.operation) {
                case ADD -> new ConfigChange.AddedRule(
                    id, timestamp, actor, ruleType,
                    objectMapper.readValue(payloadJson, Rule.class)
                );
                case UPDATE -> new ConfigChange.UpdatedRule(
                    id, timestamp, actor, ruleType,
                    objectMapper.readValue(oldPayloadJson, Rule.class),
                    objectMapper.readValue(payloadJson, Rule.class)
                );
                case DELETE -> new ConfigChange.RemovedRule(
                    id, timestamp, actor, ruleType,
                    objectMapper.readValue(payloadJson, Rule.class)
                );
            };
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing domain object", e);
        }
    }
}
