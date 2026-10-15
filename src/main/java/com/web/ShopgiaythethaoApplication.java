package com.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@EnableCaching
public class ShopgiaythethaoApplication {


    public static void main(String[] args) {
        ConfigurableApplicationContext ctx =  SpringApplication.run(ShopgiaythethaoApplication.class, args);

        String botToken = ctx.getEnvironment().getProperty("telegram.bot.token");
        String botUsername = ctx.getEnvironment().getProperty("telegram.bot.username");
        if (!StringUtils.hasText(botToken) || !StringUtils.hasText(botUsername)) {
            org.slf4j.LoggerFactory.getLogger(ShopgiaythethaoApplication.class)
                    .info("Telegram bot is not configured; skipping registration.");
            return;
        }

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(ctx.getBean("pizzaBot", AbilityBot.class));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
