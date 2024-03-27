package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepo extends JpaRepository<ProductCategory, Long> {
}
