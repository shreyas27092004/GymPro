package com.gympro.payment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Feign client for auth-service.
 * Used by PaymentService to fetch all ADMIN-role user IDs so payment
 * events (payment received, refund processed) can be broadcast to every
 * admin as an in-app notification.
 */
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    /** GET /auth/internal/admin-ids — returns every user ID with role=ADMIN. */
    @GetMapping("/auth/internal/admin-ids")
    List<Long> getAdminUserIds();
}
