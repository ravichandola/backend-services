package com.demo.payment.config;

import com.demo.payment.exception.PaymentException;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key}")
    private String key;

    @Value("${razorpay.secret}")
    private String secret;

    @PostConstruct
    public void validateCredentials() {
        if (!StringUtils.hasText(key) || key.equals("your-razorpay-key-id") ||
            !StringUtils.hasText(secret) || secret.equals("your-razorpay-secret-key")) {
            String errorMsg = """
                ⚠️  Razorpay credentials are not configured!
                
                Please set the following environment variables:
                  - RAZORPAY_KEY=your-razorpay-key-id
                  - RAZORPAY_SECRET=your-razorpay-secret-key
                
                You can set them in your terminal:
                  export RAZORPAY_KEY="rzp_test_xxxxx"
                  export RAZORPAY_SECRET="your-secret-key"
                
                Then restart the application.
                """;
            log.error(errorMsg);
            throw new PaymentException("Razorpay credentials are not configured. Please set RAZORPAY_KEY and RAZORPAY_SECRET environment variables.");
        }
        
        // Validate key format (should start with rzp_test_ or rzp_live_)
        if (!key.startsWith("rzp_test_") && !key.startsWith("rzp_live_")) {
            log.warn("Razorpay key format may be incorrect. Expected format: rzp_test_xxxxx or rzp_live_xxxxx");
        }
        
        // Log masked credentials for debugging
        String maskedKey = key.length() > 12 ? key.substring(0, 12) + "..." : key;
        log.info("✓ RazorpayClient initialized successfully with key: {}", maskedKey);
    }

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        try {
            RazorpayClient client = new RazorpayClient(key, secret);
            log.debug("RazorpayClient bean created successfully");
            return client;
        } catch (RazorpayException e) {
            log.error("Failed to initialize RazorpayClient: {}", e.getMessage());
            throw new PaymentException("Failed to initialize Razorpay client: " + e.getMessage(), e);
        }
    }
}
