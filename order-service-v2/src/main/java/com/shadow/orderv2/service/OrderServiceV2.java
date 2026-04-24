package com.shadow.orderv2.service;

import com.shadow.orderv2.model.Order;
import com.shadow.orderv2.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * v2 is the experimental implementation.
 * It introduces:
 *   1. Configurable artificial delay   → tests latency regression
 *   2. Configurable failure rate       → tests error handling
 *   3. Discount enrichment             → new business logic being validated
 *
 * These knobs are set via env vars (CHAOS_DELAY_MS / CHAOS_FAIL_RATE)
 * so you can toggle chaos without redeployment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceV2 {

    private final OrderRepository orderRepository;
    private final Random random = new Random();

    @Value("${chaos.delay-ms:0}")
    private long chaosDelayMs;

    @Value("${chaos.fail-rate:0.0}")
    private double chaosFailRate;

    @Transactional
    public Order createOrder(Order order) {
        simulateChaos();
        log.info("[v2] Creating order for customer {}", order.getCustomerId());

        // ── NEW FEATURE: automatic discount for large orders ─────────────
        if (order.getTotalAmount() != null
                && order.getTotalAmount().compareTo(BigDecimal.valueOf(100)) > 0) {
            BigDecimal discounted = order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(0.95));   // 5% off
            order.setTotalAmount(discounted);
            order.setDiscountApplied(true);
            log.debug("[v2] Discount applied → new total: {}", discounted);
        }

        return orderRepository.save(order);
    }

    public Optional<Order> findById(Long id) {
        simulateChaos();
        return orderRepository.findById(id);
    }

    public List<Order> listOrders(int page, int size) {
        simulateChaos();
        return orderRepository.findAll(PageRequest.of(page, size)).getContent();
    }

    @Transactional
    public Optional<Order> updateStatus(Long id, String status) {
        simulateChaos();
        return orderRepository.findById(id).map(order -> {
            order.setStatus(status);
            return orderRepository.save(order);
        });
    }

    @Transactional
    public void cancelOrder(Long id) {
        simulateChaos();
        orderRepository.findById(id).ifPresent(order -> {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
        });
    }

    // ── Chaos Engineering ─────────────────────────────────────────────────

    private void simulateChaos() {
        // Artificial latency
        if (chaosDelayMs > 0) {
            try {
                Thread.sleep(chaosDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Random failures
        if (chaosFailRate > 0 && random.nextDouble() < chaosFailRate) {
            log.warn("[v2] CHAOS: injecting artificial failure");
            throw new RuntimeException("Chaos: simulated v2 failure");
        }
    }
}
