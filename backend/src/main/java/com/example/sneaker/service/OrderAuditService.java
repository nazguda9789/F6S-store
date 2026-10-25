package com.example.sneaker.service;

import com.example.sneaker.model.*;
import com.example.sneaker.repository.OrderEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderAuditService {
  private final OrderEventRepository events;

  public void record(CustomerOrder order, String from, String actor, String note) {
    var e = new OrderEvent();
    e.setOrder(order);
    e.setFromStatus(from);
    e.setToStatus(order.getStatus());
    e.setActor(actor);
    e.setNote(note == null ? "" : note.trim());
    events.save(e);
  }
}
