package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.bad;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.dto.MatrixApi.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VariantMatrixService {
  private final CatalogService catalog;

  private String key(String color, String size) {
    return color.trim().toLowerCase(Locale.ROOT) + "/" + size;
  }

  public List<VariantInput> preview(Selection input) {
    var colors = input.colors().stream().map(String::trim).toList();
    var sizes = input.sizes();
    if (colors.stream().map(c -> c.toLowerCase(Locale.ROOT)).distinct().count() != colors.size()
        || new HashSet<>(sizes).size() != sizes.size()) throw bad("Màu/size không được trùng nhau");
    if (colors.size() * sizes.size() > 100) throw bad("Tối đa 100 tổ hợp cho một sản phẩm");
    var allowed = new HashSet<String>();
    for (var color : colors) for (var size : sizes) allowed.add(key(color, size));
    var overrides = new HashMap<String, com.example.sneaker.dto.MatrixApi.Override>();
    for (var row : input.overrides()) {
      String key = key(row.color(), row.size());
      if (!allowed.contains(key)) throw bad("Giá/tồn riêng không thuộc tổ hợp đã chọn");
      if (overrides.putIfAbsent(key, row) != null) throw bad("Trùng cấu hình biến thể");
    }
    var result = new ArrayList<VariantInput>();
    for (var color : colors)
      for (var size : sizes) {
        var row = overrides.get(key(color, size));
        result.add(
            new VariantInput(
                color,
                size,
                row == null ? input.defaultStock() : row.stock(),
                row == null ? input.defaultPrice() : row.price()));
      }
    return result;
  }

  public ProductView create(Create input) {
    var rows = preview(input.matrix());
    var min = rows.stream().map(VariantInput::price).min(Comparator.naturalOrder()).orElseThrow();
    return catalog.create(
        new ProductInput(
            input.name(),
            input.brand(),
            min,
            input.description(),
            input.imageUrl(),
            input.hoverImageUrl(),
            input.categoryId(),
            input.featured(),
            rows));
  }
}
