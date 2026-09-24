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
@NotNull
@Column(name="remaining",precision = 10,scale = 2,nullable = false,
insertable = false,updatable = false,columnDefinition = "numeric(10,2) not null generated always as (total-paid) stored")
private BigDecimal remaining;

@Column(name="order_number" , unique = true)
@Size(max=20)
private String orderNumber;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name="shift_id")
private ShiftSession shiftSession;

public void addOrderItem(OrderItem orderItem){

    orderItems.add(orderItem);
    orderItem.setOrder(this);

}


public void removeOrderItem(OrderItem orderItem){
    orderItems.remove(orderItem);
    orderItem.setOrder(null);
}
}
