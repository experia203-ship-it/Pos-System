package com.connectors.pos.ordersystem;

import com.connectors.pos.products.Products;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Generated;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Size(max = 255)
    @Column(name="product_name",nullable = false,length = 255)
    private String productName;
@NotNull
@Column(name="product_selling_price",precision = 10,scale = 2,nullable = false,columnDefinition = "numeric(10,2) not null default 0.00")
    private BigDecimal productSellingPrice;
@NotNull
@Column(name="product_purchase_price",precision = 10,scale = 2,nullable = false,columnDefinition = "numeric(10,2) not null default 0.00")

    private BigDecimal productPurchasePrice;
@NotNull
@PositiveOrZero
@Column(name="quantity",nullable = false,columnDefinition = "not null default 0")
    private int quantity;
@Generated
@Column(name = "sub_total",precision = 10,scale = 2,insertable = false,updatable = false,
columnDefinition = "numeric(10,2) generated always as ((quantity*product_selling_price)-sub_discount ) stored")
    private BigDecimal subTotal;
@Generated
@Column(name="sub_revenue",precision = 10,scale = 2,columnDefinition = "numeric(10,2) generated always as ( ((product_selling_price-product_purchase_price)*quantity)-sub_discount) stored",
insertable = false,updatable = false)
    private BigDecimal subRevenue;
@NotNull
@Column(name="sub_discount",precision = 5,scale = 2,nullable = false,
columnDefinition = "numeric(5,2) not null default 0.00")
    private BigDecimal subDiscount;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name="order_id",nullable = false)
    private Order order;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name="product_id")
    private Products product;


@Override

    public boolean equals(Object o){
    if (this==o) {return true;}
    if(!(o instanceof OrderItem)){return false;}

    OrderItem other= (OrderItem) o;

    return id!=null&& this.id.equals(other.getId());
}

@Override
    public int hashCode(){

    return getClass().hashCode();

}
}
