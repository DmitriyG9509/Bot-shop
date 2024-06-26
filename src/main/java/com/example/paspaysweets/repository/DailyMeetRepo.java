package com.example.paspaysweets.repository;

import com.example.paspaysweets.model.DailyMeet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyMeetRepo extends JpaRepository<DailyMeet, Long> {
    Optional<DailyMeet> deleteDailyMeetByChatId(Long chatId);
    Optional<DailyMeet> findByChatId(Long chatId);
}
