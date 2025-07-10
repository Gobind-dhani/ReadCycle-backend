package com.readcycle.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BookSummaryDto {
    private Long id;
    private String title;
    private Double sellPrice;
    private double price;
    private String imageUrl;

}