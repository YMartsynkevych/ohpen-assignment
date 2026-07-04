package com.ohpen.midoffice.configtracker.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohpen.midoffice.configtracker.api.dto.ChangeRequest;
import com.ohpen.midoffice.configtracker.domain.model.ChangeOperation;
import com.ohpen.midoffice.configtracker.domain.model.Rule;
import com.ohpen.midoffice.configtracker.domain.model.RuleType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1)
class ChangeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndRetrieveChange() throws Exception {
        Rule payload = new Rule.CreditLimitRule(new BigDecimal("5000.00"), "USD", "CUST-999");
        ChangeRequest request = new ChangeRequest(
            RuleType.CREDIT_LIMIT,
            ChangeOperation.ADD,
            "tester",
            payload,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/changes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actor").value("tester"))
                .andExpect(jsonPath("$.type").value("CREDIT_LIMIT"))
                .andExpect(jsonPath("$.payload.amount").value(5000.00));
    }
}
