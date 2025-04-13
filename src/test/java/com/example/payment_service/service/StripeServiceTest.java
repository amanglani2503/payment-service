package com.example.payment_service.service;

import com.example.payment_service.entity.Payment;
import com.example.payment_service.entity.PaymentRequest;
import com.example.payment_service.entity.StripeResponse;
import com.example.payment_service.repository.PaymentRepository;
import com.stripe.exception.ApiException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;


import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private StripeService stripeService;

    private static final String SECRET_KEY = "sk_test_1234567890";

    @BeforeEach
    void setUp() throws Exception {
        stripeService = new StripeService(paymentRepository);

        // Manually inject the secretKey using reflection
        Field secretKeyField = StripeService.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);
        secretKeyField.set(stripeService, SECRET_KEY);
    }


    @Test
    void testCheckoutProducts_Success() throws Exception {
        // Prepare request
        PaymentRequest request = new PaymentRequest();
        request.setAmount(5000); // $50.00
        request.setBookingId("BOOK123");
        request.setFlightId(456L);
        request.setCurrency("usd");
        request.setUserId("USER789");

        // Mock Stripe session
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("sess_abc123");
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/sess_abc123");

            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(mockSession);

            // Act
            StripeResponse response = stripeService.checkoutProducts(request);

            // Assert
            assertEquals("SUCCESS", response.getStatus());
            assertEquals("sess_abc123", response.getSessionId());
            assertEquals("https://checkout.stripe.com/pay/sess_abc123", response.getSessionUrl());

            verify(paymentRepository, times(1)).save(any(Payment.class));
        }
    }

    @Test
    void testCheckoutProducts_StripeException() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(5000);
        request.setBookingId("BOOK123");
        request.setFlightId(456L);
        request.setCurrency("usd");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenThrow(new ApiException("Stripe error", null, null, 400, null));
            StripeResponse response = stripeService.checkoutProducts(request);

            assertEquals("FAILED", response.getStatus());
            assertTrue(response.getMessage().contains("Stripe session creation failed"));
        }
    }
}
