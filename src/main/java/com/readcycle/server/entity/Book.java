package com.readcycle.server.entity;



import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


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
    private String publisher;
    private String isbn;
    private String language;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "book_genres", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "genre")
    private Set<String> genres;
    private Integer pages;
    private String format;
    @Column(name = "author_bio", columnDefinition = "TEXT")
    private String authorBio;
    @Column(name = "average_rating")
    private Float averageRating;
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Review> reviews = new ArrayList<>();

    // Add constructors, getters, and setters



    // Getters and Setters
    // ...
}

