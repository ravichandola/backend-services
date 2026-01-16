package com.demo.backend.service;

import com.demo.backend.dto.CreateOrderResponse;
import com.demo.backend.dto.VerifyPaymentRequest;

public interface PaymentService {
    CreateOrderResponse createOrder(Long amount);
    void verifyPayment(VerifyPaymentRequest request);
}
