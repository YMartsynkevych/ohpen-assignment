package com.ohpen.midoffice.configtracker.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.RulePayload;
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

    private LocalDateTime timestamp;
    private String actor;

    public void setPayload(RulePayload payload, ObjectMapper objectMapper) {
        try {
            this.payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing payload", e);
        }
    }

    public RulePayload getPayload(ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(payloadJson, RulePayload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing payload", e);
        }
    }
}
