package com.teensconf.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payment_receipts")
public class PaymentReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false, unique = true)
    private Registration registration;

    @Column(name = "donation_amount")
    private Double donationAmount = 500.0;

    @Column(name = "paid")
    private Boolean paid = false;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "payment_created_at")
    private LocalDateTime paymentCreatedAt;

    @PrePersist
    protected void onCreate() {
        paymentCreatedAt = LocalDateTime.now();
    }
}