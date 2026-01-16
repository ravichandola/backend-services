package com.demo.backend.controller;

import com.demo.backend.dto.CreateOrderRequest;
import com.demo.backend.dto.CreateOrderResponse;
import com.demo.backend.dto.VerifyPaymentRequest;
import com.demo.backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        return ResponseEntity.ok(
                paymentService.createOrder(request.getAmount())
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {

        paymentService.verifyPayment(request);
        return ResponseEntity.ok("Payment verified successfully");
    }
}
