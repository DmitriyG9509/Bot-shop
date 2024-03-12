package com.example.paspaysweets;

import com.example.paspaysweets.service.TelegramBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

@SpringBootApplication
@EnableScheduling
public class PaspaySweetsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaspaySweetsApplication.class, args);
    }
}
