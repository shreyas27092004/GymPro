package com.gympro.payment.service;

import com.gympro.payment.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class RazorpayServiceExtendedTest {

    private RazorpayService razorpayService;

    @BeforeEach
    void setUp() {
        razorpayService = new RazorpayService();

        ReflectionTestUtils.setField(
                razorpayService,
                "keyId",
                "rzp_test_key");

        ReflectionTestUtils.setField(
                razorpayService,
                "keySecret",
                "test_secret");
    }

    @Test
    @DisplayName("createOrder throws PaymentException when RazorpayClient is null")
    void createOrder_nullClient() {

        PaymentException ex =
                assertThrows(PaymentException.class,
                        () -> razorpayService.createOrder(500.0, "Booking"));

        assertTrue(ex.getMessage().contains("Razorpay is not configured"));
    }

    @Test
    @DisplayName("verifyPayment returns true for valid signature")
    void verifyPayment_validSignature() throws Exception {

        String orderId = "order123";
        String paymentId = "payment123";

        String data = orderId + "|" + paymentId;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                "test_secret".getBytes(),
                "HmacSHA256"));

        String signature =
                HexFormat.of().formatHex(mac.doFinal(data.getBytes()));

        assertTrue(
                razorpayService.verifyPayment(
                        orderId,
                        paymentId,
                        signature));
    }

    @Test
    @DisplayName("verifyPayment returns false for invalid signature")
    void verifyPayment_invalidSignature() {

        assertFalse(
                razorpayService.verifyPayment(
                        "order1",
                        "payment1",
                        "invalid"));
    }

    @Test
    @DisplayName("verifyPayment returns false when exception occurs")
    void verifyPayment_exception() {

        ReflectionTestUtils.setField(
                razorpayService,
                "keySecret",
                null);

        assertFalse(
                razorpayService.verifyPayment(
                        "order",
                        "payment",
                        "sig"));
    }
}