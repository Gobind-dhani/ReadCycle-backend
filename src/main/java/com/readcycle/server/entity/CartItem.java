package com.readcycle.server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookId; // ID of the book
    private String title;
    private String author;
    private double price;
    private int quantity;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
