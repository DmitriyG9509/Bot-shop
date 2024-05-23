package com.example.paspaysweets.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "sum_buy")
public class UserTotalSum {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "total_sum")
    private Long totalSum;

    @Column(name = "chat_id")
    private Long chatId;
}
