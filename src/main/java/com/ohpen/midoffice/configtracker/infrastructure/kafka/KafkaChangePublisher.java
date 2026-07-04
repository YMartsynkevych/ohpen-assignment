package com.ohpen.midoffice.configtracker.infrastructure.kafka;

import com.ohpen.midoffice.configtracker.domain.model.ConfigChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaChangePublisher {

    private final KafkaTemplate<String, ConfigChange> kafkaTemplate;
    private static final String TOPIC = "config-changes";

    public void publish(ConfigChange change) {
        log.info("Publishing change to Kafka: {}", change.id());
        kafkaTemplate.send(TOPIC, change.id().toString(), change);
    }
}
