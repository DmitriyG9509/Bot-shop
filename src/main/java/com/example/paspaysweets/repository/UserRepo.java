package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.ShopUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<ShopUser, Long> {
    Optional<ShopUser> findByChatId(Long chatId);
}
