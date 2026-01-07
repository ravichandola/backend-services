package com.demo.payment.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateOrderResponse {

    private String orderId;
    private Long amount;
    private String currency;
    private String status;
    private String receipt;
}

