package com.shadow.orderv2.model;

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
@Table(name = "orders_v2")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerId;
    private String status;
    private BigDecimal totalAmount;
    private String currency;

    /** NEW in v2 — this field is what causes body mismatches vs v1 */
    private Boolean discountApplied;

    @ElementCollection
    @CollectionTable(name = "order_v2_items",
                     joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = "PENDING";
        if (discountApplied == null) discountApplied = false;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
