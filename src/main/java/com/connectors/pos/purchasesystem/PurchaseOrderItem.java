package com.connectors.pos.purchasesystem;

import com.connectors.pos.ordersystem.Order;
import com.connectors.pos.products.Products;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="purchase_order_item")
public class PurchaseOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(name="product_name",nullable = false)
    private String productName;
    @DecimalMin(value = "0.00",inclusive = true)
    @Column(name="product_purchase_price",nullable = false,scale=2,precision = 10)
    private BigDecimal productPurchasePrice;
    private int quantity;
    @Column(name="sub_total",precision = 10,scale = 2,updatable = false,insertable = false)
    private BigDecimal subTotal;
    @Column(name="sub_discount",precision = 10,scale=2)
    private BigDecimal subDiscount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="product_id",nullable = false)
    private Products product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="order_id",nullable = false)
    private PurchaseOrder purchaseOrder;
}
