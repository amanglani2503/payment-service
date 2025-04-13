package com.example.payment_service.service;

import com.example.payment_service.entity.Payment;
import com.example.payment_service.entity.PaymentRequest;
import com.example.payment_service.entity.StripeResponse;
import com.example.payment_service.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.secretKey}")
    private String secretKey;

    private final PaymentRepository paymentRepository;

    public StripeService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

//    Creates a Stripe checkout session for processing the payment
    public StripeResponse checkoutProducts(PaymentRequest paymentRequest) {
        // Set the Stripe API key
        Stripe.apiKey = secretKey;

        try {
            // Create product metadata
            SessionCreateParams.LineItem.PriceData.ProductData productData =
                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName("Flight ID: " + paymentRequest.getFlightId())
                            .build();

            // Define price and currency
            SessionCreateParams.LineItem.PriceData priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(paymentRequest.getCurrency() != null ? paymentRequest.getCurrency() : "usd")
                            .setUnitAmount((long) paymentRequest.getAmount()) // Amount in cents
                            .setProductData(productData)
                            .build();

            // Set quantity and line item
            SessionCreateParams.LineItem lineItem =
                    SessionCreateParams.LineItem.builder()
                            .setPriceData(priceData)
                            .setQuantity(1L)
                            .build();

            // Build session params including success and cancel URLs
            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl("http://localhost:8084/success")
                            .setCancelUrl("http://localhost:8084/cancel")
                            .addLineItem(lineItem)
                            .build();

            // Create the Stripe checkout session
            Session session = Session.create(params);

            logger.info("Stripe session created successfully. Session ID: {}", session.getId());

            // Save payment details to the database
            Payment payment = Payment.builder()
                    .bookingId(paymentRequest.getBookingId())
                    .stripeSessionId(session.getId())
                    .currency(paymentRequest.getCurrency())
                    .amount((long) paymentRequest.getAmount())
                    .quantity(1L)
                    .productName("Flight ID: " + paymentRequest.getFlightId())
                    .status("CREATED")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);
            logger.info("Payment details saved for booking ID: {}", paymentRequest.getBookingId());

            // Return successful Stripe response
            return StripeResponse.builder()
                    .status("SUCCESS")
                    .message("Payment session created")
                    .sessionId(session.getId())
                    .sessionUrl(session.getUrl())
                    .build();

        } catch (StripeException e) {
            logger.error("Stripe session creation failed: {}", e.getMessage(), e);
            // Return failure response
            return StripeResponse.builder()
                    .status("FAILED")
                    .message("Stripe session creation failed: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Unexpected error while creating Stripe session", e);
            // Return failure response
            return StripeResponse.builder()
                    .status("FAILED")
                    .message("Unexpected error: " + e.getMessage())
                    .build();
        }
    }
}
