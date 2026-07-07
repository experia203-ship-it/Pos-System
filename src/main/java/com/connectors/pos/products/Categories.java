package com.connectors.pos.products;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@SQLDelete(sql = "update category set is_active = false where id=?")
@SQLRestriction("is_active=true")
@Table(name="category" , indexes = {
        @Index(name="index_category_is_active_true",columnList = "id")

})
public class Categories {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Size(max = 50)
    @Column(name="name",nullable = false,length = 50)
    private String name;

@Size(max = 255)
@Column(name="description" ,length = 255)
    private String description;

@OneToMany(mappedBy = "category",orphanRemoval = true,cascade = CascadeType.ALL)
@Builder.Default
private Set<Products> products = new HashSet<>();
@Builder.Default
@NotNull
@Column(name="is_active",nullable = false)
private boolean active =true;
public void addProduct(Products product){
this.products.add(product);
product.setCategory(this);
}

public void removeProduct(Products product){

    this.products.remove(product);
    product.setCategory(null);

}
}
