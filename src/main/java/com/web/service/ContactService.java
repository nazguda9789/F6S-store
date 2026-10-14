package com.web.service;
import com.web.bot.TeleSend;
import com.web.entity.Contact;
import com.web.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContactService {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private TeleSend teleSend;

    public static int newCounter = 0;


    public ContactService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public Contact save(Contact contact) {
        Contact c = contactRepository.save(contact);
        ++newCounter;
        messagingTemplate.convertAndSend("/topic/contact", newCounter);
        teleSend.sendMessage("Có liên hệ mới:\nHọ tên: "+c.getFullName()+"\nNội dung: "+c.getContent());
        return c;
    }

    public List<Contact> findAll() {
        return contactRepository.findAll();
    }

    public Contact findById(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy liên hệ với id: " + id));
    }

    public Page<Contact> findAllByAdmin(LocalDate from, LocalDate to, String search, Pageable pageable){
        search = "%"+search+"%";
        Page<Contact> page = null;

        if(from == null || to == null){
            page = contactRepository.findByParam(search, pageable);
        }
        else{
            LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : null;
            LocalDateTime toDateTime = (to != null) ? to.atTime(23, 59, 59) : null;
            page = contactRepository.findByParamAndDate(search, fromDateTime, toDateTime, pageable);
        }
        newCounter = 0;
        return page;
    }

    public void deleteById(Long id){
        contactRepository.deleteById(id);
    }
}
