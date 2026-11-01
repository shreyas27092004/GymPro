package com.gympro.payment.service;

import com.gympro.payment.dto.RazorpayOrderResponse;
import com.gympro.payment.exception.PaymentException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

// ✅ Razorpay Service
//
// How Razorpay works (3 steps):
// STEP 1: Backend creates an "order" in Razorpay with amount → gets orderId
// STEP 2: Frontend shows Razorpay payment popup using orderId
// STEP 3: After payment, frontend sends orderId + paymentId + signature back
// STEP 4: Backend VERIFIES the signature to confirm payment is real
//
// This class handles Steps 1 and 4.
@Slf4j
@Service
public class RazorpayService {

    @Autowired(required = false)  // required=false because client can be null if keys aren't set
    private RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    // STEP 1: Create a Razorpay order
    // amount = in rupees (e.g. 999.0)
    // Returns orderId which the frontend uses to open payment popup
    public RazorpayOrderResponse createOrder(Double amount, String description) {

        if (razorpayClient == null) {
            throw new PaymentException(
                "Razorpay is not configured. Please add your API keys to application.properties. " +
                "For testing, use paymentMethod=UPI or CASH instead."
            );
        }

        try {
            // Razorpay amounts are in PAISE (1 rupee = 100 paise)
            int amountInPaise = (int) (amount * 100);

            // Build the order request JSON
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "GYMPRO-" + System.currentTimeMillis());
            orderRequest.put("notes", new JSONObject().put("description", description));

            // Create order via Razorpay API
            Order order = razorpayClient.orders.create(orderRequest);

            log.info("✅ Razorpay order created: {} for ₹{}", order.get("id"), amount);

            return new RazorpayOrderResponse(
                order.get("id").toString(),
                amount,
                "INR",
                order.get("status").toString(),
                keyId
            );

        } catch (RazorpayException e) {
            log.error("❌ Razorpay order creation failed: {}", e.getMessage());
            throw new PaymentException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    // STEP 4: Verify payment signature
    // Razorpay sends back: orderId + paymentId + signature
    // We verify the signature using HMAC-SHA256 to confirm payment is genuine
    // (Prevents fake payment confirmations)
    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        try {
            // Create the string Razorpay signed: orderId|paymentId
            String data = orderId + "|" + paymentId;

            // Generate HMAC-SHA256 signature using our secret key
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                keySecret.getBytes("UTF-8"), "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(data.getBytes("UTF-8"));

            // Convert to hex string
            String generatedSignature = HexFormat.of().formatHex(hashBytes);

            // Compare our signature with what Razorpay sent
            boolean isValid = generatedSignature.equals(signature);

            if (isValid) {
                log.info("✅ Payment signature verified for order: {}", orderId);
            } else {
                log.warn("❌ Invalid payment signature for order: {}", orderId);
            }

            return isValid;

        } catch (Exception e) {
            log.error("❌ Signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
