package com.example.bai_02.service;

import com.example.bai_02.entity.Order;
import com.example.bai_02.event.PaymentResponseEvent;
import com.example.bai_02.exception.OrderNotFoundException;
import com.example.bai_02.responsitory.OrderRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResponseListener {

    private final OrderRepository orderRepository;

    public PaymentResponseListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @EventListener
    public void handlePaymentResponse(PaymentResponseEvent event) {

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(
                        () -> new OrderNotFoundException(
                                event.getOrderId()
                        )
                );

        if (!"PENDING".equals(order.getStatus())) {
            return;
        }

        switch (event.getStatus().toUpperCase()) {

            case "SUCCESS":

                order.setStatus("PAID");
                orderRepository.save(order);

                System.out.println(
                        "Payment SUCCESS -> Order "
                                + order.getId()
                                + " chuyển sang PAID"
                );

                break;

            case "REJECTED":

                order.setStatus("CANCELED");
                orderRepository.save(order);

                System.out.println(
                        "Payment REJECTED -> Order "
                                + order.getId()
                                + " chuyển sang CANCELED"
                );

                break;

            case "FAILED":

                order.setStatus("FAILED");
                orderRepository.save(order);

                System.out.println(
                        "Payment FAILED -> Order "
                                + order.getId()
                                + " chuyển sang FAILED"
                );

                break;

            default:

                System.out.println(
                        "Payment status không xác định: "
                                + event.getStatus()
                );

                break;
        }
    }
}
