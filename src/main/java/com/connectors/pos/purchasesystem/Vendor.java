package com.connectors.pos.purchasesystem;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="vendor")
@SQLRestriction("is_active = true")
public class Vendor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="name",nullable = false)
   @NotNull
    private String name;
    @Column(name="location")
    private String location;
    @Column(name="landline")
    private String landline;
    @Column(name="mobile")
    private String mobile;
    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;
}
