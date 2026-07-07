package com.gympro.payment.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// ✅ Razorpay Configuration
//
// @Configuration = this class provides Spring beans
// @Bean = creates a RazorpayClient object that can be @Autowired anywhere
//
// RazorpayClient is the official Razorpay Java SDK object.
// It uses your API key + secret to authenticate with Razorpay's servers.
@Slf4j
@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() {
        try {
            log.info("✅ Initializing Razorpay client with key: {}...", keyId.substring(0, 8));
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            // If Razorpay key is invalid, log error but don't crash the app
            // The payment service can still handle cash/UPI without Razorpay
            log.error("⚠️ Razorpay client init failed: {}. Dummy payment will be used.", e.getMessage());
            return null;  // Null means we'll use dummy payment logic
        }
    }
}
