package com.example.paspaysweets.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity(name = "transactions")
public class Transactions {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "price")
    private Long price;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;
}
