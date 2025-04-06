package com.example.payment_service.service;

import com.example.payment_service.entity.Payment;
import com.example.payment_service.entity.PaymentRequest;
import com.example.payment_service.entity.StripeResponse;
import com.example.payment_service.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StripeService {

    @Value("${stripe.secretKey}")
    private String secretKey;

    private final PaymentRepository paymentRepository;

    public StripeService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public StripeResponse checkoutProducts(PaymentRequest paymentRequest) {
        Stripe.apiKey = secretKey;

        // Prepare product and price data
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName("Flight ID: " + paymentRequest.getFlightId())
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(paymentRequest.getCurrency() != null ? paymentRequest.getCurrency() : "usd")
                        .setUnitAmount((long) paymentRequest.getAmount())
                        .setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams.LineItem.builder()
                        .setPriceData(priceData)
                        .setQuantity(1L)
                        .build();

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl("http://localhost:8080/success")
                        .setCancelUrl("http://localhost:8080/cancel")
                        .addLineItem(lineItem)
                        .build();

        try {
            Session session = Session.create(params);

            // Save to DB
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

            return StripeResponse.builder()
                    .status("SUCCESS")
                    .message("Payment session created")
                    .sessionId(session.getId())
                    .sessionUrl(session.getUrl())
                    .build();

        } catch (StripeException e) {
            e.printStackTrace();
            return StripeResponse.builder()
                    .status("FAILED")
                    .message("Stripe session creation failed: " + e.getMessage())
                    .build();
        }
    }
}
//
//
//package com.example.payment_service.service;
//
//import com.example.payment_service.entity.PaymentRequest;
//import com.example.payment_service.entity.StripeResponse;
//import com.stripe.Stripe;
//import com.stripe.model.checkout.Session;
//import com.stripe.param.checkout.SessionCreateParams;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//public class StripeService {
//
//    @Value("${stripe.secret.key}")
//    private String secretKey;
//
//    @Value("${frontend.success.url}")
//    private String successUrl;
//
//    @Value("${frontend.cancel.url}")
//    private String cancelUrl;
//
//    public StripeResponse checkoutProducts(PaymentRequest request) {
//        try {
//            Stripe.apiKey = secretKey;
//
//            Map<String, String> metadata = new HashMap<>();
//            metadata.put("bookingId", request.getBookingId());
//
//            SessionCreateParams params = SessionCreateParams.builder()
//                    .setMode(SessionCreateParams.Mode.PAYMENT)
//                    .setSuccessUrl(successUrl)
//                    .setCancelUrl(cancelUrl)
//                    .addLineItem(
//                            SessionCreateParams.LineItem.builder()
//                                    .setQuantity(1L)
//                                    .setPriceData(
//                                            SessionCreateParams.LineItem.PriceData.builder()
//                                                    .setCurrency(request.getCurrency())
//                                                    .setUnitAmount((long) (request.getAmount() * 100)) // in cents
//                                                    .setProductData(
//                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
//                                                                    .setName("Flight booking for " + request.getUserId())
//                                                                    .build()
//                                                    )
//                                                    .build()
//                                    )
//                                    .build()
//                    )
//                    .putMetadata("bookingId", request.getBookingId())
//                    .build();
//
//            Session session = Session.create(params);
//
//            StripeResponse response = new StripeResponse();
//            response.setSessionId(session.getId());
//            response.setSessionUrl(session.getUrl());
//            response.setStatus("SUCCESS");
//            response.setMessage("Session created successfully");
//
//            return response;
//
//        } catch (Exception e) {
//            StripeResponse response = new StripeResponse();
//            response.setStatus("FAILED");
//            response.setMessage("Stripe session creation failed: " + e.getMessage());
//            return response;
//        }
//    }
//}
