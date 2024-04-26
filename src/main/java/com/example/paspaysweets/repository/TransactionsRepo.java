package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionsRepo extends JpaRepository<Transactions, Long> {
}
