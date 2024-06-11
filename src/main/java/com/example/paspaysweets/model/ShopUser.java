package com.example.paspaysweets.model;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity(name = "userDataTable")
public class ShopUser {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "name")
    private String name;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "fio")
    private String fio;

    @Column(name = "registered_at")
    private Timestamp registeredAt;

    @Column(name = "duty")
    private Long duty;

    @Column(name = "cash")
    private Long cash;

    @Column(name = "total_sum")
    private Long totalSum;

    public String getUsername() {
        return userName;
    }

    @PrePersist
    public void prePersist() {
        this.cash = 0L;
        this.duty = 0L;
    }
}
