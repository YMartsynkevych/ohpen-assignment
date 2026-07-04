package com.ohpen.midoffice.configtracker.infrastructure.kafka;

import com.ohpen.midoffice.configtracker.domain.model.ConfigChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConfigChangeEventListener {

    private final KafkaChangePublisher kafkaPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleConfigChangeEvent(ConfigChangeEvent event) {
        log.info("Handling config change event after commit: {}", event.getChange().id());
        kafkaPublisher.publish(event.getChange());
    }
}
