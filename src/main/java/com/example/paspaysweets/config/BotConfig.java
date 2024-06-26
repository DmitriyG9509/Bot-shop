package com.example.paspaysweets.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@Data
@ConfigurationProperties(prefix = "bot")
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
    @Value("${bot.owner.3}")
    private Long owner3;
    @Value("${bot.hr.1}")
    private Long hr1;
    @Value("${bot.hr.2}")
    private Long hr2;
    @Value("${bot.admin.1}")
    private Long admin1;


    public List<Long> getBotOwners() {
        return Arrays.asList(owner1, owner2, owner3);
    }
    public List<Long> getBotHr() {return Arrays.asList(hr1, hr2, owner1, owner3); }
    public List<Long> getBotAdmin() {return Arrays.asList(admin1, owner1, owner3); }
}