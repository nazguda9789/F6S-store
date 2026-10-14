package com.web.bot;

import com.web.repository.KeyTelegramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@EnableAsync
public class TeleSend {

    private final KeyTelegramRepository keyTelegramRepository;
    private final PizzaBot pizzaBot;

    @Autowired
    public TeleSend(KeyTelegramRepository keyTelegramRepository, PizzaBot pizzaBot) {
        this.keyTelegramRepository = keyTelegramRepository;
        this.pizzaBot = pizzaBot;
    }

    @Async
    public void sendMessage(String message) {
        List<Long> chatIds = keyTelegramRepository.findAllActive();
        for (Long chatId : chatIds) {
            pizzaBot.sendMessage(chatId, message);
        }
    }
}
