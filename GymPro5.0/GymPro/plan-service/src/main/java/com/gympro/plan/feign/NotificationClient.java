package com.gympro.plan.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// ✅ Feign client – calls notification-service to send plan emails
@FeignClient(name = "notification-service")
public interface NotificationClient {

    // POST /notify/plan/subscribed
    @PostMapping("/notify/plan/subscribed")
    String sendPlanSubscriptionEmail(@RequestParam("to")        String to,
                                     @RequestParam("planName")  String planName,
                                     @RequestParam("startDate") String startDate,
                                     @RequestParam("endDate")   String endDate);

    // POST /notify/send — generic fallback
    @PostMapping("/notify/send")
    String sendNotification(@RequestParam("email")   String email,
                            @RequestParam("subject") String subject,
                            @RequestParam("message") String message);
}
