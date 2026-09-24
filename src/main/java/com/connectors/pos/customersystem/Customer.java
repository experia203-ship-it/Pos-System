package com.connectors.pos.customersystem;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="customer")
@SQLRestriction("is_active = true")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Size(max = 50)
    @Column(name="name",nullable = false,length = 50)
    private String name;

    @NotNull
    @Size(max=1000)
    @Column(name="location",nullable = false, length=1000)
    private String location;

    @NotNull
    @Size(max=50)
    @Column(name="shipping_company",nullable = false,length=50)
    private String shippingCompany;

    @OneToMany(mappedBy = "customer",cascade = CascadeType.ALL,orphanRemoval = true)
   @Builder.Default
    private Set<CustomerPhone> customerPhones= new HashSet<>();
    @Column(name="is_active")
    @Builder.Default
    private boolean isActive=true  ;

    public void addCustomerPhone(CustomerPhone customerPhone){

        customerPhones.add(customerPhone);
        customerPhone.setCustomer(this);
    }

    public void removeCustomerPhone(CustomerPhone customerPhone){

        customerPhones.remove(customerPhone);
        customerPhone.setCustomer(null);
    }



}
