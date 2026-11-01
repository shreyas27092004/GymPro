package com.gympro.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// ✅ Feign client — calls notification-service to send the OTP email
@FeignClient(name = "notification-service")
public interface NotificationClient {

    // POST /notify/send — generic email (used to send OTP)
    @PostMapping("/notify/send")
    String sendEmail(@RequestParam("email")   String email,
                     @RequestParam("subject") String subject,
                     @RequestParam("message") String message);
}
