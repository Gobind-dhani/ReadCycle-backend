package com.readcycle.server.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Book being sold
    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    // User Details
    private String name;
    private String phone;
    private String email;

    // Address Details
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private String landmark;
    private String Awb;


    // Pickup Slot
    private String pickupSlot;

    // Payment Info
    private String paymentMethod; // "UPI" or "BANK"

    // For UPI
    private String upiName;
    private String upiId;
    private String upiPhone;

    // For BANK
    private String bankAccountName;
    private String bankAccountNumber;
    private String ifscCode;

    // Status (Optional)
    private String status = "PENDING"; // default
}
