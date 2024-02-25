package com.example.paspaysweets.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Arrays;
import java.util.List;

@Configuration
@Data
@PropertySource("application.yaml")
@ComponentScan("com.example.paspaysweets")
public class BotConfig {
    @Value("${bot.name}")
    String botName;
    @Value("${bot.token}")
    String token;
    @Value("${bot.owner.1}")
    private Long owner1;
    @Value("${bot.owner.2}")
    private Long owner2;
    public List<Long> getBotOwners() {
        return Arrays.asList(owner1);
    }
}