package com.connectors.pos.products;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="products" ,indexes = {
        @Index(name = "idx_name_products",columnList = "name"),
        @Index(name="idx_description_products",columnList = "description")
})
@SQLDelete(sql = "update products set is_active = false where id=?")
@SQLRestriction("is_active=true")
public class Products {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max=255)
    @Column(name="name", length=255 , unique = true,nullable = false)
    private String name;
    @NotNull
    @Size(max=50)
    @Column(name="part_number",length = 50,nullable = false,unique = true)
    private String partNumber;
    @NotNull
    @Size(max=255)
    @Column(name="description",nullable = false,length=255)
    private String description;
    @Positive
    @Column(name="selling_price",precision = 2,scale = 10)
    private BigDecimal sellingPrice;
   @Positive
   @Column(name="purchase_price",precision = 2,scale = 10)
    private BigDecimal purchasePrice;
@PositiveOrZero
@Builder.Default
@Column(name="stock",columnDefinition = "bigint default 0")
    private Long stock =0L;

@Column(name="created_at",columnDefinition = "timestamp default current_timestamp",
insertable = false,updatable = false)
@Generated
    private LocalDateTime createdAt ;

@Column(name="updated_at")
    private LocalDateTime updatedAt;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name="category_id")

    private Categories category;
@Builder.Default
@NotNull
@Column(name="is_active",nullable = false)
private boolean active = true;

@Override
public boolean equals(Object o){

    if(this==o) return true;
    if(!(o instanceof Products)) return false;

    Products other = (Products) o;

    return id!=null&& id.equals(other.getId());


}

public int hashCode(){


    return getClass().hashCode();
}

}
