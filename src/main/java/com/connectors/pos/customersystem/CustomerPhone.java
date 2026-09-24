package com.connectors.pos.customersystem;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name="customer_phone")
public class CustomerPhone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name ="phone_number",length = 50,nullable = false)
    private String phoneNumber;
    @NotNull
    @Column(name="type",length = 50)
@Enumerated(EnumType.STRING)
    private Type type;
@NotNull
@Column(name="is_primary",columnDefinition = "boolean not null default false")
private boolean isPrimary = false;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name="customer_id",nullable=false)
private Customer customer;



    @Override
    public boolean equals(Object o){
        if(this==o){return true;}
        if(!(o instanceof CustomerPhone)){return false;}
        CustomerPhone other = (CustomerPhone)  o;

        return id!=null && this.id.equals(other.id);
    }
    @Override
    public int hashCode() {

        return getClass().hashCode();

    }

}
