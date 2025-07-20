package com.readcycle.server.repository;

import com.readcycle.server.entity.SellCartItem;
import com.readcycle.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellCartItemRepository extends JpaRepository<SellCartItem, Long> {
    List<SellCartItem> findByUser(User user);
    void deleteByUser(User user);
}
