package com.connectors.pos.purchasesystem;

import com.connectors.pos.users.Users;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="purchase_order")
public class PurchaseOrder {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)

    Long id ;
@NotNull
@Column(name="user_name",nullable = false)
private String username;
@DecimalMin(value = "0.00",inclusive = true)
@Column(scale =2,precision = 10,name="total",nullable = false,columnDefinition = "not null default 0.00")
private BigDecimal total = BigDecimal.ZERO;
@CreationTimestamp
@NotNull
@Column(name="created_at",nullable = false)
private LocalDateTime createdAt;
    @Column(scale =2,precision = 10,name="discount",nullable = false,columnDefinition = "not null default 0.00")
private BigDecimal discount=BigDecimal.ZERO ;
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id",nullable = false)
    private Users user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
private Vendor vendor;
    @Column(name="paid",precision = 10,scale = 2)
   private BigDecimal paid = BigDecimal.ZERO;
   @Transient
   private BigDecimal remaining;

    @Column(name = "vendor_credit", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal vendorCredit = BigDecimal.ZERO;

    @Column(name="order_number" , unique = true)
    @Size(max=20)
    private String orderNumber;
@Builder.Default
@OneToMany(mappedBy = "purchaseOrder",cascade = CascadeType.ALL,orphanRemoval = true)
private List<PurchaseOrderItem> items = new ArrayList<>();

public BigDecimal getRemaining() {
    BigDecimal netTotal = total == null ? BigDecimal.ZERO : total;
    BigDecimal orderDiscount = discount == null ? BigDecimal.ZERO : discount;
    BigDecimal paidAmount = paid == null ? BigDecimal.ZERO : paid;
    BigDecimal credits = vendorCredit == null ? BigDecimal.ZERO : vendorCredit;
    return netTotal.subtract(orderDiscount).subtract(paidAmount).subtract(credits);
}

public void addItem(PurchaseOrderItem item){

    items.add(item);
    item.setPurchaseOrder(this);
}

public void removeItem(PurchaseOrderItem item){

    items.remove(item);
    item.setPurchaseOrder(null);
}

}
