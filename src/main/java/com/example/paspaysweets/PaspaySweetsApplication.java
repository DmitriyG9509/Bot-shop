package com.example.paspaysweets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaspaySweetsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaspaySweetsApplication.class, args);
    }
}
