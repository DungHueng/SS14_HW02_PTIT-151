package com.example.bai_02.event;

public class PaymentResponseEvent {

    private Long orderId;
    private String status;

    public PaymentResponseEvent() {
    }

    public PaymentResponseEvent(
            Long orderId,
            String status
    ) {
        this.orderId = orderId;
        this.status = status;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}