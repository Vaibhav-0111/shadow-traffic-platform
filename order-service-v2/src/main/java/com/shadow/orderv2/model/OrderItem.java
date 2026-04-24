package com.shadow.orderv2.model;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Embeddable
public class OrderItem {
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
}
