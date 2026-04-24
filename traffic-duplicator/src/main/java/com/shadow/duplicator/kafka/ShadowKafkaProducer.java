package com.shadow.duplicator.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadow.duplicator.model.ShadowPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShadowKafkaProducer {

    public static final String TOPIC = "shadow.traffic.pairs";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishShadowPair(ShadowPair pair) {
        try {
            String payload = objectMapper.writeValueAsString(pair);
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(TOPIC, pair.getRequestId(), payload);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Published shadow pair [{}] to partition {}",
                            pair.getRequestId(),
                            result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish shadow pair [{}]: {}",
                            pair.getRequestId(), ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Serialization error for pair [{}]: {}", pair.getRequestId(), e.getMessage());
        }
    }
}
