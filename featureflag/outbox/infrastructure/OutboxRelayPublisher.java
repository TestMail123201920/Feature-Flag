package com.company.featureflag.outbox.infrastructure;

import com.company.featureflag.outbox.domain.OutboxEvent;
import com.company.featureflag.outbox.domain.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Relays {@link OutboxEvent} rows written in the same transaction as the
 * domain change they describe (spec §20) out to Kafka. Runs on a fixed
 * delay rather than reacting synchronously to writes, so a Kafka outage
 * never blocks or fails the write path (spec §21) — events simply
 * accumulate as PENDING and drain once Kafka is back.
 *
 * Send is synchronous (bounded by a short timeout) per event within a
 * modest batch size, which keeps this simple and correct; if throughput
 * ever demands it, swapping to async sends with a completion callback is a
 * contained change local to this class.
 */
@Component
public class OutboxRelayPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayPublisher.class);
    private static final String TOPIC = "feature-flag.events";
    private static final int MAX_RETRIES_BEFORE_FAILED = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final int batchSize;
    private final Counter publishedCounter;
    private final Counter publishFailureCounter;

    public OutboxRelayPublisher(OutboxEventRepository outboxEventRepository,
                                 KafkaTemplate<String, Object> kafkaTemplate,
                                 MeterRegistry meterRegistry,
                                 @Value("${feature-flag.outbox.batch-size:100}") int batchSize) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
        this.publishedCounter = Counter.builder("feature_flag.outbox.published")
                .description("Outbox events successfully published to Kafka")
                .register(meterRegistry);
        this.publishFailureCounter = Counter.builder("feature_flag.outbox.publish_failures")
                .description("Outbox publish attempts that failed (will retry unless retry limit exceeded)")
                .register(meterRegistry);
        meterRegistry.gauge("feature_flag.outbox.pending", outboxEventRepository,
                repo -> repo.countByStatus(OutboxStatus.PENDING));
    }

    @Scheduled(fixedDelayString = "${feature-flag.outbox.relay-interval-ms:2000}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, batchSize));

        for (OutboxEvent event : pending) {
            try {
                Map<String, Object> message = Map.of(
                        "eventId", event.getEventId().toString(),
                        "eventType", event.getEventType(),
                        "aggregateType", event.getAggregateType(),
                        "aggregateId", event.getAggregateId().toString(),
                        "payload", event.getPayload());

                kafkaTemplate.send(TOPIC, event.getAggregateId().toString(), message)
                        .get(5, TimeUnit.SECONDS);
                event.markPublished();
                publishedCounter.increment();
            } catch (Exception ex) {
                event.markFailedAttempt();
                publishFailureCounter.increment();
                if (event.getRetryCount() >= MAX_RETRIES_BEFORE_FAILED) {
                    log.error("Outbox event {} exceeded retry limit, marking FAILED", event.getEventId(), ex);
                } else {
                    log.warn("Failed to publish outbox event {} (attempt {}), will retry: {}",
                            event.getEventId(), event.getRetryCount(), ex.getMessage());
                }
            }
            outboxEventRepository.save(event);
        }
    }
}
