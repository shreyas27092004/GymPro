package com.gympro.payment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// ✅ Feign Client for Notification Service
//
// @FeignClient(name = "notification-service")
//   → Spring creates an HTTP client that calls notification-service
//   → "notification-service" is the name it's registered with in Eureka
//   → No manual RestTemplate needed!
@FeignClient(name = "notification-service")
public interface NotificationClient {

    // Calls POST /notify/send on notification-service
    @PostMapping("/notify/send")
    String sendNotification(@RequestParam("email")   String email,
                            @RequestParam("subject") String subject,
                            @RequestParam("message") String message);

    // Calls POST /notify/payment/receipt on notification-service
    @PostMapping("/notify/payment/receipt")
    String sendPaymentReceipt(@RequestParam("to")          String to,
                              @RequestParam("amount")      Double amount,
                              @RequestParam("method")      String method,
                              @RequestParam("txnId")       String txnId,
                              @RequestParam("description") String description);

    // Calls POST /notify/payment/refund on notification-service
    @PostMapping("/notify/payment/refund")
    String sendRefundEmail(@RequestParam("to")     String to,
                           @RequestParam("amount") Double amount,
                           @RequestParam("txnId")  String txnId);
}
