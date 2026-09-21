package com.example.bai_02.service;

import com.example.bai_02.entity.Order;
import com.example.bai_02.responsitory.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderTimeoutScheduler {

    private final OrderRepository orderRepository;

    public OrderTimeoutScheduler(
            OrderRepository orderRepository
    ) {
        this.orderRepository = orderRepository;
    }
    
    @Scheduled(fixedRate = 60000)
    public void processExpiredPendingOrders() {

        LocalDateTime timeoutThreshold =
                LocalDateTime.now().minusMinutes(5);

        List<Order> expiredOrders =
                orderRepository
                        .findByStatusAndCreatedAtBefore(
                                "PENDING",
                                timeoutThreshold
                        );

        for (Order order : expiredOrders) {

            // Timeout -> FAILED
            order.setStatus("FAILED");

            orderRepository.save(order);

            System.out.println(
                    "Order "
                            + order.getId()
                            + " bị timeout quá 5 phút "
                            + "-> chuyển sang FAILED"
            );

            // TODO:
            // Gọi Inventory Service để hoàn kho
            //
            // TODO:
            // Có thể gọi Payment Service/Gateway
            // để kiểm tra trạng thái thanh toán trước
            // khi quyết định hủy đơn.
        }
    }
}
