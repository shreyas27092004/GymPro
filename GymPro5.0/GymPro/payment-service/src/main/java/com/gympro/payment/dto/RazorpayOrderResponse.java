package com.gympro.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// DTO returned after creating a Razorpay order
// The client uses orderId to open the Razorpay payment popup
@Data
@AllArgsConstructor
public class RazorpayOrderResponse {
    private String orderId;       // Razorpay order ID (e.g. order_ABC123)
    private Double amount;        // Amount in rupees
    private String currency;      // Always "INR"
    private String status;        // created / attempted / paid
    private String keyId;         // Razorpay key ID (sent to frontend)
}
