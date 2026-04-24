package com.shadow.comparator.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadow.comparator.model.ComparisonResult;
import com.shadow.comparator.service.ComparisonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listens on the shadow.traffic.pairs topic.
 * For every pair received it triggers the comparator and persists the result.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShadowPairConsumer {

    private final ComparisonService comparisonService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics  = "shadow.traffic.pairs",
        groupId = "comparator-group",
        concurrency = "3"          // 3 parallel consumer threads
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String requestId = record.key();
        log.debug("Received shadow pair [{}] from partition {} offset {}",
                requestId, record.partition(), record.offset());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(record.value(), Map.class);
            ComparisonResult result = comparisonService.compare(payload);

            log.info("[{}] Comparison complete — match={} latencyDelta={}ms",
                    requestId, result.isMatch(), result.getLatencyDeltaMs());

            ack.acknowledge();   // manual ack only after successful processing
        } catch (Exception e) {
            log.error("Failed to process shadow pair [{}]: {}", requestId, e.getMessage(), e);
            // Don't ack — message will be retried by Kafka
        }
    }
}
