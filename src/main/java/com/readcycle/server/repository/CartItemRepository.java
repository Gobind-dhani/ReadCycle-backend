package com.readcycle.server.repository;

import com.readcycle.server.entity.CartItem;
import com.readcycle.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    void deleteByUser(User user);
}
