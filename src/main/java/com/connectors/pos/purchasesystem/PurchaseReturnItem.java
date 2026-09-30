package com.connectors.pos.purchasesystem;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_return_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_return_id", nullable = false)
    private PurchaseReturn purchaseReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_item_id", nullable = false)
    private PurchaseOrderItem purchaseOrderItem;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_credit", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCredit;

    @Column(name = "credit_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal creditTotal;
}
