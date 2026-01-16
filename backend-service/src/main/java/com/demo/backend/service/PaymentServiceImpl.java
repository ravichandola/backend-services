package com.demo.backend.service;

import com.demo.backend.dto.CreateOrderResponse;
import com.demo.backend.dto.VerifyPaymentRequest;
import com.demo.backend.entity.PaymentOrder;
import com.demo.backend.entity.PaymentTransaction;
import com.demo.backend.exception.PaymentException;
import com.demo.backend.repository.PaymentOrderRepository;
import com.demo.backend.repository.PaymentTransactionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentOrderRepository orderRepo;
    private final PaymentTransactionRepository txRepo;

    @Value("${razorpay.secret}")
    private String razorpaySecret;

    @Override
    public CreateOrderResponse createOrder(Long amount) {
        if (amount == null || amount <= 0) {
            throw new PaymentException("Invalid amount. Amount must be greater than 0");
        }

        if (razorpayClient == null) {
            throw new PaymentException("Razorpay is not configured. Please set RAZORPAY_KEY and RAZORPAY_SECRET.");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", amount * 100); // Convert to paise
            options.put("currency", "INR");
            options.put("receipt", "rcpt_" + System.currentTimeMillis());

            log.info("Creating Razorpay order for amount: {} INR", amount);
            Order order = razorpayClient.orders.create(options);
            String orderId = (String) order.get("id");
            log.info("Razorpay order created successfully: {}", orderId);

            PaymentOrder paymentOrder = PaymentOrder.builder()
                    .razorpayOrderId((String) order.get("id"))
                    .amount(amount)
                    .currency("INR")
                    .status("CREATED")
                    .build();
            
            orderRepo.save(paymentOrder);
            log.debug("Payment order saved to database: {}", paymentOrder.getId());

            return CreateOrderResponse.builder()
                    .orderId((String) order.get("id"))
                    .amount(((Number) order.get("amount")).longValue() / 100) // Convert back to rupees
                    .currency((String) order.get("currency"))
                    .status((String) order.get("status"))
                    .receipt((String) order.get("receipt"))
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay API error while creating order: {}", e.getMessage());
            String errorMessage = e.getMessage();
            
            if (errorMessage != null && errorMessage.contains("Authentication failed")) {
                throw new PaymentException("Razorpay authentication failed. Please verify your RAZORPAY_KEY and RAZORPAY_SECRET are correct.");
            } else if (errorMessage != null && errorMessage.contains("BAD_REQUEST")) {
                throw new PaymentException("Invalid request to Razorpay: " + errorMessage);
            } else {
                throw new PaymentException("Failed to create Razorpay order: " + errorMessage);
            }
        } catch (Exception e) {
            log.error("Unexpected error while creating order", e);
            throw new PaymentException("Order creation failed: " + e.getMessage());
        }
    }

    @Override
    public void verifyPayment(VerifyPaymentRequest request) {
        if (request == null || request.getRazorpayOrderId() == null || 
            request.getRazorpayPaymentId() == null || request.getRazorpaySignature() == null) {
            throw new PaymentException("Invalid payment verification request. All fields are required.");
        }

        if (razorpaySecret == null || razorpaySecret.isEmpty()) {
            throw new PaymentException("Razorpay is not configured. Please set RAZORPAY_SECRET.");
        }

        try {
            String payload = request.getRazorpayOrderId()
                    + "|" + request.getRazorpayPaymentId();

            log.info("Verifying payment for order: {}", request.getRazorpayOrderId());
            String generatedSignature = Utils.getHash(payload, razorpaySecret);

            if (!generatedSignature.equals(request.getRazorpaySignature())) {
                log.error("Payment signature verification failed for order: {}", request.getRazorpayOrderId());
                throw new PaymentException("Invalid payment signature. Payment verification failed.");
            }

            log.info("Payment signature verified successfully for order: {}", request.getRazorpayOrderId());

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .razorpayOrderId(request.getRazorpayOrderId())
                    .razorpayPaymentId(request.getRazorpayPaymentId())
                    .razorpaySignature(request.getRazorpaySignature())
                    .status("SUCCESS")
                    .build();
            
            txRepo.save(transaction);
            log.debug("Payment transaction saved: {}", transaction.getId());

            PaymentOrder order = orderRepo
                    .findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElseThrow(() -> {
                        log.error("Order not found for verification: {}", request.getRazorpayOrderId());
                        return new PaymentException("Order not found: " + request.getRazorpayOrderId());
                    });

            order.setStatus("PAID");
            orderRepo.save(order);
            log.info("Order status updated to PAID: {}", order.getId());

        } catch (PaymentException e) {
            throw e; // Re-throw PaymentException as-is
        } catch (Exception e) {
            log.error("Unexpected error during payment verification", e);
            throw new PaymentException("Payment verification failed: " + e.getMessage());
        }
    }
}
