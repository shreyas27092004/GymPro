package com.gympro.payment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign client for booking-service.
 * Used by PaymentService to mark a booking's paymentStatus=COMPLETED
 * right after a payment (Razorpay or cash/dummy) succeeds. Without this
 * call, Booking.paymentStatus stays PENDING forever even though the
 * Payment record itself is SUCCESS.
 */
@FeignClient(name = "booking-service")
public interface BookingServiceClient {

    /** POST /bookings/{id}/confirm-payment — marks a PAID booking's paymentStatus=COMPLETED. */
    @PostMapping("/bookings/{id}/confirm-payment")
    Object confirmPayment(@PathVariable("id") Long bookingId);
}
