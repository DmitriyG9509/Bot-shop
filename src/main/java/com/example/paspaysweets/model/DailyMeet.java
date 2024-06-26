package com.example.paspaysweets.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "daily_meet")
public class DailyMeet {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id")
    private Long chatId;
}
