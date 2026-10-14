package com.web.bot;


import com.web.service.KeyTelegramService;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import java.util.Map;

import static com.web.bot.Constants.START_TEXT;
import static com.web.bot.UserState.*;


@Service
public class ResponseHandler {

    private final SilentSender sender;
    private final Map<Long, UserState> chatStates;

    private final KeyTelegramService keyTelegramService;

    public ResponseHandler(SilentSender sender, DBContext db, KeyTelegramService keyTelegramService) {
        this.sender = sender;
        chatStates = db.getMap(Constants.CHAT_STATES);
        this.keyTelegramService = keyTelegramService;
    }

    public void replyToStart(long chatId, String groupName) {
        System.out.println("===== chat ID ========"+ chatId);
        System.out.println("===== groupName ========"+ groupName);
        String result = keyTelegramService.saveOrUpdate(chatId, groupName);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(START_TEXT+"\n"+result);
        sender.execute(message);
    }

    public void replyCus(long chatId, Message message) {
        String text = message.getText().trim();
        if(text.equalsIgnoreCase("/start")){
            replyToStart(chatId, message.getChat().getTitle());
        }
        else if(text.startsWith("key: ")){
            String key = text.substring(5).trim();
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            if(keyTelegramService.checkKey(chatId, key)){
                sendMessage.setText("✅ Đã kích hoạt nhận thông báo thành công!");
            }
            else{
                sendMessage.setText("❌ Kích hoạt nhận thông báo thất bại ");
            }
            sender.execute(sendMessage);
        }
        else{
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("❌ Lệnh không phù hợp");
            sender.execute(sendMessage);
        }
    }

    public void sendMessage(long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        sender.execute(sendMessage);
    }


    private void stopChat(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Đã kết thúc\nCảm ơn bạn đã tương tác");
        chatStates.remove(chatId);
        sendMessage.setReplyMarkup(new ReplyKeyboardRemove(true));
        sender.execute(sendMessage);
    }

    private void replyToOrder(long chatId, Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if ("yes".equalsIgnoreCase(message.getText())) {
            sendMessage.setText("We will deliver it soon. Thank you!\nOrder another?");
            sendMessage.setReplyMarkup(KeyboardFactory.getPizzaOrDrinkKeyboard());
            sender.execute(sendMessage);
            chatStates.put(chatId, FOOD_DRINK_SELECTION);
        } else if ("no".equalsIgnoreCase(message.getText())) {
            stopChat(chatId);
        } else {
            sendMessage.setText("Please select yes or no");
            sendMessage.setReplyMarkup(KeyboardFactory.getYesOrNo());
            sender.execute(sendMessage);
        }
    }

    private void replyToPizzaToppings(long chatId, Message message) {
        if ("margherita".equalsIgnoreCase(message.getText())) {
            promptWithKeyboardForState(chatId, "You selected Margherita Pizza.\nWe will deliver it soon. Thank you!\nOrder again?",
                    KeyboardFactory.getYesOrNo(), AWAITING_CONFIRMATION);
        } else if ("pepperoni".equalsIgnoreCase(message.getText())) {
            promptWithKeyboardForState(chatId, "We finished the Pepperoni Pizza.\nSelect another Topping",
                    KeyboardFactory.getPizzaToppingsKeyboard(), PIZZA_TOPPINGS);
        } else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("We don't sell " + message.getText() + " Pizza.\nSelect the toppings!");
            sendMessage.setReplyMarkup(KeyboardFactory.getPizzaToppingsKeyboard());
            sender.execute(sendMessage);
        }
    }

    private void promptWithKeyboardForState(long chatId, String text, ReplyKeyboard YesOrNo, UserState awaitingReorder) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(YesOrNo);
        sender.execute(sendMessage);
        chatStates.put(chatId, awaitingReorder);
    }

    private void replyToFoodDrinkSelection(long chatId, Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if ("drink".equalsIgnoreCase(message.getText())) {
            sendMessage.setText("We don't sell drinks.\nBring your own drink!! :)");
            sendMessage.setReplyMarkup(KeyboardFactory.getPizzaOrDrinkKeyboard());
            sender.execute(sendMessage);
        } else if ("pizza".equalsIgnoreCase(message.getText())) {
            sendMessage.setText("We love Pizza in here.\nSelect the toppings!");
            sendMessage.setReplyMarkup(KeyboardFactory.getPizzaToppingsKeyboard());
            sender.execute(sendMessage);
            chatStates.put(chatId, UserState.PIZZA_TOPPINGS);
        } else {
            sendMessage.setText("We don't sell " + message.getText() + ". Please select from the options below.");
            sendMessage.setReplyMarkup(KeyboardFactory.getPizzaOrDrinkKeyboard());
            sender.execute(sendMessage);
        }
    }

    private void replyToName(long chatId, Message message) {
        promptWithKeyboardForState(chatId, "Hello " + message.getText() + ". What would you like to have?",
                KeyboardFactory.getPizzaOrDrinkKeyboard(),
                UserState.FOOD_DRINK_SELECTION);
    }

    public boolean userIsActive(Long chatId) {
        return chatStates.containsKey(chatId);
    }
}