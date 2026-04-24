package com.shadow.orderv1.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders_v1")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerId;
    private String status;          // PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    private BigDecimal totalAmount;
    private String currency;

    @ElementCollection
    @CollectionTable(name = "order_v1_items",
                     joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = "PENDING";
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
