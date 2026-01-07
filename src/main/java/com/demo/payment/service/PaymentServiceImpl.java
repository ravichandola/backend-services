package com.demo.payment.service;

import com.demo.payment.dto.CreateOrderResponse;
import com.demo.payment.dto.VerifyPaymentRequest;
import com.demo.payment.entity.PaymentOrder;
import com.demo.payment.entity.PaymentTransaction;
import com.demo.payment.exception.PaymentException;
import com.demo.payment.repository.PaymentOrderRepository;
import com.demo.payment.repository.PaymentTransactionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

        try {
            JSONObject options = new JSONObject();
            options.put("amount", amount * 100);
            options.put("currency", "INR");
            options.put("receipt", "rcpt_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(options);

            orderRepo.save(
                    PaymentOrder.builder()
                            .razorpayOrderId(order.get("id"))
                            .amount(amount)
                            .currency("INR")
                            .status("CREATED")
                            .build()
            );

            return CreateOrderResponse.builder()
                    .orderId(order.get("id"))
                    .amount(((Number) order.get("amount")).longValue())
                    .currency(order.get("currency"))
                    .status(order.get("status"))
                    .receipt(order.get("receipt"))
                    .build();


        } catch (Exception e) {
            e.printStackTrace();
            throw new PaymentException("Order creation failed " +e.getMessage());
        }
    }

    @Override
    public void verifyPayment(VerifyPaymentRequest request) {

        try {
            String payload = request.getRazorpayOrderId()
                    + "|" + request.getRazorpayPaymentId();

            String generatedSignature =
                    Utils.getHash(payload, razorpaySecret);

            if (!generatedSignature.equals(request.getRazorpaySignature())) {
                throw new PaymentException("Invalid payment signature");
            }

            txRepo.save(
                    PaymentTransaction.builder()
                            .razorpayOrderId(request.getRazorpayOrderId())
                            .razorpayPaymentId(request.getRazorpayPaymentId())
                            .razorpaySignature(request.getRazorpaySignature())
                            .status("SUCCESS")
                            .build()
            );

            PaymentOrder order = orderRepo
                    .findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElseThrow(() -> new PaymentException("Order not found"));

            order.setStatus("PAID");
            orderRepo.save(order);

        } catch (Exception e) {
            throw new PaymentException("Payment verification failed");
        }
    }
}

