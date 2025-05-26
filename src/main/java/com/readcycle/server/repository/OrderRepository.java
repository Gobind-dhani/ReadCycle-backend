package com.readcycle.server.repository;

import com.readcycle.server.entity.Order;
import com.readcycle.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Order> findByAwb(String awb);
}
