package com.example.bai_02.responsitory;

import com.example.bai_02.entity.Order;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final List<Order> orders = new ArrayList<>();

    public Optional<Order> findById(Long orderId) {

        return orders.stream()
                .filter(order ->
                        order.getId().equals(orderId))
                .findFirst();
    }

    public Order save(Order order) {

        orders.removeIf(existingOrder ->
                existingOrder.getId()
                        .equals(order.getId())
        );

        orders.add(order);

        return order;
    }

    public List<Order> findByStatusAndCreatedAtBefore(
            String status,
            LocalDateTime time
    ) {

        return orders.stream()
                .filter(order ->
                        status.equals(order.getStatus())
                                && order.getCreatedAt() != null
                                && order.getCreatedAt()
                                .isBefore(time)
                )
                .toList();
    }
}