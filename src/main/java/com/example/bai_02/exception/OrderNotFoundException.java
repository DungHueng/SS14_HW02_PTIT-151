package com.example.bai_02.exception;

public class OrderNotFoundException
        extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super(
                "Không tìm thấy Order với ID: "
                        + orderId
        );
    }
}