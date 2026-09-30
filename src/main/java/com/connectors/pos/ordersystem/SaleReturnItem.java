package com.connectors.pos.ordersystem;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_return_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_return_id", nullable = false)
    private SaleReturn saleReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "restocked", nullable = false)
    private boolean restocked;

    @Column(name = "credit_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal creditTotal;

    @Column(name = "unit_credit", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCredit;
}
