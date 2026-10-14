package com.web.repository;

import com.web.entity.KeyTelegram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface KeyTelegramRepository extends JpaRepository<KeyTelegram, Long> {

    @Query("select k from KeyTelegram k where k.chatId = ?1")
    Optional<KeyTelegram> findByChatId(Long chatId);

    @Query("select k from KeyTelegram k where k.chatId = ?1 and k.keyActive = ?2")
    Optional<KeyTelegram> findByChatAndKey(Long chatId, String key);

    @Query("select k.chatId from KeyTelegram k where k.active = true")
    List<Long> findAllActive();
}
