package com.demo.backend.service.payment;

import com.demo.backend.dto.payment.CreateOrderResponse;
import com.demo.backend.dto.payment.VerifyPaymentRequest;

public interface PaymentService {
    CreateOrderResponse createOrder(Long amount);
    void verifyPayment(VerifyPaymentRequest request);
}
