package com.readcycle.server.repository;

import com.readcycle.server.entity.Order;
import com.readcycle.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
}
