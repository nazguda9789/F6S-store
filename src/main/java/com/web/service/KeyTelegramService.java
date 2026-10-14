package com.web.service;

import com.web.entity.KeyTelegram;
import com.web.repository.KeyTelegramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class KeyTelegramService {

    @Autowired
    private KeyTelegramRepository keyTelegramRepository;

    public String saveOrUpdate(Long chatId, String groupName){
        Optional<KeyTelegram> optional = keyTelegramRepository.findByChatId(chatId);
        if(optional.isEmpty()){
            KeyTelegram telegram = new KeyTelegram();
            telegram.setChatId(chatId);
            telegram.setKeyActive(genKeyRandom());
            telegram.setGroupName(groupName);
            keyTelegramRepository.save(telegram);
            return "--------------------------\n✅ Đã lưu thông tin nhóm chat vào CSDL\nLiên hệ với admin và nhập key: xxxxxx để kích hoạt nhóm chat";
        }
        else{
            if(optional.get().getActive() == true){
                return "---------------- \n❌ Nhóm chat đã được lưu vào CSDL";
            }
            else{
                return "---------------- \n❌ Nhóm chat đã được lưu vào CSDL\nLiên hệ với admin và nhập key: xxxxxx để kích hoạt nhóm chat";
            }
        }
    }

    public Boolean checkKey(Long chatId, String key){
        Optional<KeyTelegram> optional = keyTelegramRepository.findByChatAndKey(chatId, key);
        if(optional.isPresent()){
            optional.get().setActive(true);
            keyTelegramRepository.save(optional.get());
            return true;
        }
        return false;
    }

    public String genKeyRandom(){
        String key = UUID.randomUUID().toString();
        Random random = new Random();
        int number = 100 + random.nextInt(900);
        key = key +"-" + number;
        return key;
    }
}
