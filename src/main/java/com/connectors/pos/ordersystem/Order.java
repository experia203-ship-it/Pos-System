package com.connectors.pos.ordersystem;

import com.connectors.pos.shift.ShiftSession;
import com.connectors.pos.customersystem.Customer;
import com.connectors.pos.users.Users;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Size(max = 50)
    @Column(name="user_name",length = 50,nullable = false)
    private String userName;
    @NotNull
    @PositiveOrZero
    @Column(name="total",nullable = false,precision =10,scale = 2,
    columnDefinition = "numeric(10,2) not null default 0.00")
    private BigDecimal total;
    @NotNull
    @Column(name="discount",precision = 5,scale = 2,nullable = false,
    columnDefinition = "numeric(5,2) not null default 0.00")
    private BigDecimal discount;
    @Builder.Default
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 32)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;
    @Column(name = "payment_reference", length = 100)
    private String paymentReference;
    @Builder.Default
    @Column(name = "cash_received", nullable = false, precision = 10, scale = 2)
    private BigDecimal cashReceived = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "cash_change", nullable = false, precision = 10, scale = 2)
    private BigDecimal cashChange = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "return_credit", nullable = false, precision = 10, scale = 2)
    private BigDecimal returnCredit = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "refunded_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundedTotal = BigDecimal.ZERO;
    @Column(name = "voided", nullable = false)
    private boolean voided;
@NotNull
@Column(name="revenue",nullable = false,precision = 10,scale =2,
columnDefinition = "numeric(10,2) not null default 0.00")
    private BigDecimal revenue;

@CreationTimestamp
@Column(name="created_at",columnDefinition = "timestamp default current_timestamp",updatable = false)
    private LocalDateTime createdAt;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "user_id",nullable = false)
    private Users user;

@ManyToOne(fetch=FetchType.LAZY)
@JoinColumn(name="customer_id")
   private Customer customer;
@Builder.Default
@OneToMany(mappedBy = "order",cascade = CascadeType.ALL,orphanRemoval = true)
private Set<OrderItem> orderItems = new HashSet<>();
@NotNull
@Column(name="paid",nullable=false,precision = 10,scale =2,columnDefinition = "Numeric(10,2) not null default 0 check(paid>=0)")
private BigDecimal paid;
@Transient
private BigDecimal remaining;

@Column(name="order_number" , unique = true)
@Size(max=20)
private String orderNumber;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name="shift_id")
private ShiftSession shiftSession;

public BigDecimal getRemaining() {
    BigDecimal balance = zeroIfNull(total).subtract(zeroIfNull(paid))
            .subtract(zeroIfNull(returnCredit));
    return balance.max(BigDecimal.ZERO);
}

private static BigDecimal zeroIfNull(BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount;
}

public void addOrderItem(OrderItem orderItem){

    orderItems.add(orderItem);
    orderItem.setOrder(this);

}


public void removeOrderItem(OrderItem orderItem){
    orderItems.remove(orderItem);
    orderItem.setOrder(null);
}
}
