package com.demo.backend.config;

import com.demo.backend.exception.PaymentException;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key:}")
    private String key;

    @Value("${razorpay.secret:}")
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
                
                You can set them in your .env file or as environment variables.
                """;
            log.warn(errorMsg);
            // Don't throw exception - allow app to start without Razorpay (optional feature)
        } else {
            // Validate key format (should start with rzp_test_ or rzp_live_)
            if (!key.startsWith("rzp_test_") && !key.startsWith("rzp_live_")) {
                log.warn("Razorpay key format may be incorrect. Expected format: rzp_test_xxxxx or rzp_live_xxxxx");
            }
            
            // Log masked credentials for debugging
            String maskedKey = key.length() > 12 ? key.substring(0, 12) + "..." : key;
            log.info("✓ RazorpayClient initialized successfully with key: {}", maskedKey);
        }
    }

    @Bean
    @ConditionalOnExpression("!'${razorpay.key:}'.isEmpty() && !'${razorpay.secret:}'.isEmpty() && '${razorpay.key:}' != 'your-razorpay-key-id' && '${razorpay.secret:}' != 'your-razorpay-secret-key'")
    public RazorpayClient razorpayClient() throws RazorpayException {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(secret)) {
            log.warn("Razorpay credentials not provided. Payment features will not work.");
            return null; // This should not be reached due to @ConditionalOnExpression
        }
        
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
