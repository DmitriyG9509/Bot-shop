package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepo extends JpaRepository<Product, Long> {
}
