package com.demo.payment.service;

import com.demo.payment.dto.CreateOrderResponse;
import com.demo.payment.dto.VerifyPaymentRequest;

public interface PaymentService {
    CreateOrderResponse createOrder(Long amount);
    void verifyPayment(VerifyPaymentRequest request);
}
