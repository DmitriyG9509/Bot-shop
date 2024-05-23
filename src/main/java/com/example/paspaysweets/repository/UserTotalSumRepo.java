package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.UserTotalSum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTotalSumRepo extends JpaRepository<UserTotalSum, Long> {

    Optional<UserTotalSum> findByChatId(Long chatId);
}
