package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.ShopUser;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepo extends JpaRepository<ShopUser, Long> {
    Optional<ShopUser> findByChatId(Long chatId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM user_data_table WHERE user_name = :userName", nativeQuery = true)
    void deleteByUserName(@Param("userName") String userName);
}
