package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.*;

import com.example.sneaker.dto.CatalogAdminApi.*;
import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShoeAttributeService {
  private final ShoeAttributeRepository attributes;
  private final ProductRepository products;
  private final VariantRepository variants;

  public List<AttributeView> list(String type, boolean includeInactive) {
    if (!Set.of("BRAND", "SOLE", "MATERIAL", "COLOR", "SIZE").contains(type))
      throw bad("Loại thuộc tính không hợp lệ");
    return attributes.findByTypeOrderByNameAsc(type).stream()
        .filter(a -> includeInactive || a.isActive())
        .map(ShoeAttributeService::view)
        .toList();
  }

  @Transactional
  public AttributeView create(AttributeInput i) {
    var a = new ShoeAttribute();
    a.setCode("TT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
    apply(a, i);
    attributes.saveAndFlush(a);
    return view(a);
  }

  @Transactional
  public AttributeView update(Long id, AttributeInput input) {
    var a = attributes.findById(id).orElseThrow(() -> missing("Không tìm thấy thuộc tính"));
    if (!a.getType().equals(input.type())) throw bad("Không đổi nhóm thuộc tính đã tạo");
    if (!a.getName().equals(input.name().trim()) && used(a))
      throw bad("Thuộc tính đang dùng: chỉ đổi trạng thái, không đổi tên làm sai biến thể/lịch sử");
    apply(a, input);
    attributes.flush();
    return view(a);
  }

  private void apply(ShoeAttribute a, AttributeInput i) {
    String name = i.name().trim();
    if (i.type().equals("SIZE") && !name.matches("(?:3[5-9]|4[0-9]|50)(?:\\.5)?"))
      throw bad("Size EU phải từ 35 đến 50, hỗ trợ nửa size");
    if (i.type().equals("COLOR") && name.length() > 60) throw bad("Tên màu tối đa 60 ký tự");
    a.setType(i.type());
    a.setName(name);
    a.setActive(i.active());
  }

  private boolean used(ShoeAttribute a) {
    return switch (a.getType()) {
      case "BRAND" -> products.existsByBrandIgnoreCase(a.getName());
      case "COLOR" -> variants.existsByColorIgnoreCase(a.getName());
      case "SIZE" -> variants.existsBySize(a.getName());
      case "SOLE" -> products.existsBySoleAttributeId(a.getId());
      case "MATERIAL" -> products.existsByMaterialAttributeId(a.getId());
      default -> false;
    };
  }

  public ShoeAttribute require(Long id, String type) {
    if (id == null) return null;
    var a = attributes.findById(id).orElseThrow(() -> bad("Không tìm thấy thuộc tính"));
    if (!a.isActive() || !a.getType().equals(type))
      throw bad("Thuộc tính sai loại hoặc đã ngừng dùng");
    return a;
  }

  public static AttributeView view(ShoeAttribute a) {
    return a == null
        ? null
        : new AttributeView(a.getId(), a.getCode(), a.getType(), a.getName(), a.isActive());
  }
}
