package com.example.paspaysweets;

import com.example.paspaysweets.config.BotConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
public class PaspaySweetsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaspaySweetsApplication.class, args);
    }

}
