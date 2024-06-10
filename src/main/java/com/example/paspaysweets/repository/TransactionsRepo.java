package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionsRepo extends JpaRepository<Transactions, Long> {
    List<Transactions> findAllByName(String name);
    Optional<Transactions> findFirstByOrderByIdAsc();
}
