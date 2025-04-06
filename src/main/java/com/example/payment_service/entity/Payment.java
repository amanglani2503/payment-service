package com.example.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bookingId;
    private String stripeSessionId;
    private String stripePaymentIntentId;
    private String currency;
    private Long amount;
    private Long quantity;
    private String productName;

    private String status; // e.g., CREATED, SUCCESS, FAILED, CANCELLED
    private String receiptUrl; // Optional: for invoice link

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}