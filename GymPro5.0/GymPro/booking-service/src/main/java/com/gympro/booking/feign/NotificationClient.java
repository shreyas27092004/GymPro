package com.gympro.booking.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/notify/send")
    String sendNotification(@RequestParam("email")   String email,
                            @RequestParam("subject") String subject,
                            @RequestParam("message") String message);

    @PostMapping("/notify/booking/member")
    String sendBookingConfirmationToMember(@RequestParam("to")          String to,
                                           @RequestParam("bookingId")   Long bookingId,
                                           @RequestParam("trainerName") String trainerName,
                                           @RequestParam("day")         String day,
                                           @RequestParam("time")        String time);

    @PostMapping("/notify/booking/trainer")
    String sendBookingNotificationToTrainer(@RequestParam("to")         String to,
                                            @RequestParam("bookingId")  Long bookingId,
                                            @RequestParam("memberName") String memberName,
                                            @RequestParam("day")        String day,
                                            @RequestParam("time")       String time);

    @PostMapping("/notify/booking/cancel")
    String sendCancellationEmail(@RequestParam("to")        String to,
                                 @RequestParam("bookingId") Long bookingId);
}