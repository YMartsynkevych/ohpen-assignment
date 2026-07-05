package com.ohpen.midoffice.configtracker.infrastructure.persistence;

import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rule_states")
@Data
@NoArgsConstructor
public class RuleStateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private String tenantId;

    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    private String ruleKey;

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    private LocalDateTime lastModified;
}
