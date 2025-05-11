package com.readcycle.server.entity;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private double price;
    private String condition;
    private String description;
    @Column(name = "image_url")
    private String imageUrl;

    // Add constructors, getters, and setters



    // Getters and Setters
    // ...
}

