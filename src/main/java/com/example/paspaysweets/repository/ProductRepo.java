package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepo extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);
    Optional<Product> findById(Long id);
}
