package com.ohpen.midoffice.configtracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigurationTest {

    private final JacksonConfiguration jacksonConfiguration = new JacksonConfiguration();

    @Test
    void shouldConfigureObjectMapperCorrectly() {
        // When
        ObjectMapper objectMapper = jacksonConfiguration.objectMapper();

        // Then
        assertThat(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
        
        // Verify JavaTimeModule is registered by checking if it can serialize LocalDateTime as string
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 0);
        String json = objectMapper.valueToTree(now).asText();
        assertThat(json).isEqualTo("2024-01-01T12:00:00");
    }
}
