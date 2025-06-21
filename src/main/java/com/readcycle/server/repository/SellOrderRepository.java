package com.readcycle.server.repository;

import com.readcycle.server.entity.SellOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellOrderRepository extends JpaRepository<SellOrder, Long> {
}
