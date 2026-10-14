package com.web.repository;
import com.web.entity.Chatting;
import com.web.entity.User;
import com.web.enums.ChattingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ChatRepository extends JpaRepository<Chatting, Long> {

    @Query("select c from Chatting c where c.user.id = ?1")
    List<Chatting> findByUser(Long userId);

    @Query("select distinct c.user from Chatting c where c.user.email like ?1 or c.user.fullName like ?1 ")
    public List<User> getAllUserChat(String param);
}