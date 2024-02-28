package com.example.paspaysweets.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "productTable")
public class Product {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "price")
    private Long price;

    @Column(name = "quantity")
    private Long quantity;
}
