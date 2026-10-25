package com.example.sneaker.config;

import com.example.sneaker.dto.CatalogAdminApi.AttributeInput;
import com.example.sneaker.repository.ShoeAttributeRepository;
import com.example.sneaker.service.ShoeAttributeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Order(-10)
public class AttributeBootstrap implements CommandLineRunner {
  private final ShoeAttributeRepository attributes;
  private final ShoeAttributeService service;
  private final ReferenceCatalog catalog;

  @Override
  @Transactional
  public void run(String... args) {
    if (attributes.count() != 0) return;
    catalog.data().products().stream()
        .map(ReferenceCatalog.Row::brand)
        .distinct()
        .forEach(name -> service.create(new AttributeInput("BRAND", name, true)));
    for (String name : List.of("Cao su", "EVA", "Phylon", "TPU", "Foam"))
      service.create(new AttributeInput("SOLE", name, true));
    for (String name : List.of("Mesh", "Da tổng hợp", "Da tự nhiên", "Canvas", "Knit"))
      service.create(new AttributeInput("MATERIAL", name, true));
    for (var color : catalog.data().colors())
      service.create(new AttributeInput("COLOR", color.name(), true));
    for (var size : catalog.data().sizes()) service.create(new AttributeInput("SIZE", size, true));
  }
}
