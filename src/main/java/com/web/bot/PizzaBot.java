package com.web.bot;


import com.web.service.KeyTelegramService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.BiConsumer;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;
import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

@Component
public class PizzaBot extends AbilityBot {

    private ResponseHandler responseHandler;

    @Autowired
    private KeyTelegramService keyTelegramService;

    public PizzaBot(Environment environment) {
        super(environment.getProperty("telegram.bot.token"), environment.getProperty("telegram.bot.username"));
    }

    @PostConstruct
    public void init() {
        responseHandler = new ResponseHandler(silent, db, keyTelegramService);
    }

    public Ability startBot() {
        return Ability
                .builder()
                .name("start")
                .info(Constants.START_DESCRIPTION)
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> responseHandler.replyToStart(ctx.chatId(), ctx.update().getMessage().getChat().getTitle()))
                .build();
    }

    public Reply replayCus() {
        BiConsumer<BaseAbilityBot, Update> action = (abilityBot, upd) -> responseHandler.replyCus(getChatId(upd), upd.getMessage());
        return Reply.of(action, Flag.TEXT,upd -> true);
    }



    public void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        silent.execute(sendMessage);
    }

    @Override
    public long creatorId() {
        return 1L;
    }
}