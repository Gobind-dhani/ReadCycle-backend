package com.readcycle.server.entity;



import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String providerId; // e.g., GitHub ID
    private String provider;   // e.g., "github"
    private String name;
    private String email;
    private String avatarUrl;
}
