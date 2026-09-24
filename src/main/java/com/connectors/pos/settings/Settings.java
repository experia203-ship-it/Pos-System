package com.connectors.pos.settings;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="settings")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Settings {
    @Id
    @Column(name="id")
    private int id =1;
    @NotNull
     @Size(max = 255)
    @Column(name="company_name",nullable = false,length = 255 ,columnDefinition = "not null default 'my company'")
    private String companyName="my company";
@Size(max=13)
@Column( name="phone_number",length=13)
    private String phoneNumber;
@Column(name="address")
private String address;
@Column(name="tax_registration_number")
private String taxRegistrationNumber;

@Column(name="default_theme",length=50,columnDefinition = "varchar(50) default 'SYSTEM_DEFAULT'")
@NotNull(message="theme is required")
@Enumerated(EnumType.STRING)
private Theme theme=Theme.SYSTEM_DEFAULT;
@Column(name="print_size",length=50,columnDefinition = "varchar(50) default 'A5'")
@Enumerated(EnumType.STRING)
private PrintSize printSize = PrintSize.A5;
@Size(max=50)
@Column(name="currency_symbol", length=50,columnDefinition = "varchar(50) default 'EGP'")
private String currencySymbol ="EGP";
@Enumerated(EnumType.STRING)
private PosStyle  posStyle = PosStyle.HORIZONTAL;
@Column(name="logo")
private byte[] logo;
@Column(name="shift_management",nullable = false)
private boolean shiftManagement = true;

    @Column(name = "is_licensed")
    private boolean licensed = false;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
}

