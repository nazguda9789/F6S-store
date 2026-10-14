package com.web.api.user;

import com.web.entity.Chatting;
import com.web.entity.User;
import com.web.enums.ChattingType;
import com.web.repository.ChatRepository;
import com.web.repository.UserRepository;
import com.web.service.UserService;
import com.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chat-realtime")
public class ChatRealtimeApi {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserUtils userUtils;

    private final SimpMessagingTemplate messagingTemplate;

    public ChatRealtimeApi(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }


    @GetMapping("/user/my-chat")
    public ResponseEntity<?> myChat(){
        List<Chatting> result = chatRepository.findByUser(userUtils.getUserWithAuthority().getId());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/admin/getAllUserChat")
    public ResponseEntity<?> getAllUserChat(@RequestParam(value = "search", required = false) String search){
        if(search == null){
            search = "";
        }
        search = "%"+search +"%";
        List<User> list = chatRepository.getAllUserChat(search);
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @GetMapping("/admin/getListChat")
    public List<Chatting> getListChat(@RequestParam("idUser") Long idUser){
        return chatRepository.findByUser(idUser);
    }

    @PostMapping("/user/post")
    public ResponseEntity<?> chatByUser(@RequestBody Chatting chatting){
        chatting.setUser(userUtils.getUserWithAuthority());
        chatting.setType(ChattingType.USER);
        chatting.setCreatedDate(LocalDateTime.now());
        chatRepository.save(chatting);
        messagingTemplate.convertAndSend("/topic/chat-by-staff", chatting);
        return new ResponseEntity<>(chatting, HttpStatus.OK);
    }

    @PostMapping("/employee-admin/post")
    public ResponseEntity<?> chatByStaff(@RequestBody Chatting chatting){
        chatting.setType(ChattingType.STAFF);
        chatting.setCreatedDate(LocalDateTime.now());
        chatRepository.save(chatting);
        messagingTemplate.convertAndSend("/topic/chat-by-user/"+chatting.getUser().getId(), chatting);
        return new ResponseEntity<>(chatting, HttpStatus.OK);
    }

}
