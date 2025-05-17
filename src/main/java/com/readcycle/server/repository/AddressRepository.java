package com.readcycle.server.repository;

import com.readcycle.server.entity.Address;
import com.readcycle.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUser(User user);
}