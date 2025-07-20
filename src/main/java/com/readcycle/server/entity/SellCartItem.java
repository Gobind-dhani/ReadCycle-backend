package com.readcycle.server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sell_cart_items")
public class SellCartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookId; // ID of the book
    private String title;
    private String author;
    private double sellPrice;
    private int quantity;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
