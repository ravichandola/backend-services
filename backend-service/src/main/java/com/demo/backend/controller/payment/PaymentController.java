package com.demo.backend.controller.payment;

import com.demo.backend.dto.payment.CreateOrderRequest;
import com.demo.backend.dto.payment.CreateOrderResponse;
import com.demo.backend.dto.payment.VerifyPaymentRequest;
import com.demo.backend.service.payment.PaymentService;
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
