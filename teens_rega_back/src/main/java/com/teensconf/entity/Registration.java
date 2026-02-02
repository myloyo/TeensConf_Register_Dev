package com.teensconf.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "registrations")
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(name = "birth_date", nullable = false)
    private String birthDate;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String telegram;

    @Column(nullable = false)
    private String city;

    @Column(name = "need_accommodation")
    private Boolean needAccommodation = false;

    @Column(nullable = false)
    private String church;

    @Column(nullable = false)
    private String role;

    @Column(name = "parent_full_name")
    private String parentFullName;

    @Column(name = "parent_phone")
    private String parentPhone;

    @Column(name = "consent_under_14")
    private Boolean consentUnder14 = false;

    @Column(name = "consent_donation")
    private Boolean consentDonation = false;

    @Column(name = "consent_personal_data")
    private Boolean consentPersonalData = false;

    @Column(name = "was_before")
    private Boolean wasBefore = false;

    @Column(name = "registration_created_at")
    private LocalDateTime registrationCreatedAt;

    @Column(name = "registration_completed_at")
    private LocalDateTime registrationCompletedAt;

    @OneToOne(mappedBy = "registration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PaymentReceipt paymentReceipt;

    @PrePersist
    protected void onCreate() {
        registrationCreatedAt = LocalDateTime.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}