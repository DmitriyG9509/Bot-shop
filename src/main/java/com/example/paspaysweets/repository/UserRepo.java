package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.ShopUser;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<ShopUser, Long> {
    Optional<ShopUser> findByChatId(Long chatId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM user_data_table WHERE user_name = :userName", nativeQuery = true)
    void deleteByUserName(@Param("userName") String userName);
    @Modifying
    @Transactional
    @Query("UPDATE userDataTable u SET u.duty = u.duty - :amount WHERE u.chatId = :chatId")
    void deductAmountFromDuty(@Param("amount") Long amount, @Param("chatId") Long chatId);

    @Modifying
    @Transactional
    @Query("UPDATE userDataTable u SET u.cash = u.cash + :amount WHERE u.chatId = :chatId")
    void increaseCash(@Param("amount") Long amount, @Param("chatId") Long chatId);

    boolean existsByUserName(String userName);

    Optional<ShopUser> findByUserName(String userName);
}
