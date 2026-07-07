package com.connectors.pos.users;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")

public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    @NotNull
    @Size(max = 50)
    @Column(name = "name",nullable = false,length = 50)
    private String name;
    @Email
    @NotNull
    @Column(name="email",nullable = false,unique = true)
    private String email;
    @NotNull
    @Column(name = "password",nullable = false)
    private String password;
    @Column(name = "created_at",columnDefinition = "timestamp default current_timestamp",
    insertable = false,updatable = false)
    @Generated(event = EventType.INSERT)
    private LocalDateTime createdAt;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id",nullable = false),
            inverseJoinColumns = @JoinColumn(name = "role_id",nullable = false)
    )
    @Builder.Default
    private Set<Roles> roles = new HashSet<>();
}
