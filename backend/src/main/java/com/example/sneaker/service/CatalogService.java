package com.example.sneaker.service;

import com.example.sneaker.dto.Api.*;
import com.example.sneaker.model.*;
import com.example.sneaker.repository.*;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final ShoeAttributeService attributes;
  private final MediaService media;

  public PageView<ProductView> search(
      Long category,
      String brand,
      BigDecimal min,
      BigDecimal max,
      String size,
      String color,
      String q,
      Boolean featured,
      String sort,
      int page,
      int limit) {
    if (min != null && max != null && min.compareTo(max) > 0)
      throw bad("Giá tối thiểu phải nhỏ hơn giá tối đa");
    Specification<Product> filter =
        (root, query, cb) -> {
          var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
          predicates.add(cb.isTrue(root.get("active")));
          predicates.add(cb.isTrue(root.get("category").get("active")));
          if (category != null) predicates.add(cb.equal(root.get("category").get("id"), category));
          if (brand != null && !brand.isBlank())
            predicates.add(
                cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase(Locale.ROOT)));
          if (featured != null) predicates.add(cb.equal(root.get("featured"), featured));
          if (q != null && !q.isBlank()) {
            String escaped =
                q.trim()
                    .toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_")
                    .replace("[", "\\[");
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '\\'));
          }
          if (min != null
              || max != null
              || (size != null && !size.isBlank())
              || (color != null && !color.isBlank())) {
            var variant = root.join("variants", JoinType.INNER);
            if (min != null) predicates.add(cb.greaterThanOrEqualTo(variant.get("price"), min));
            if (max != null) predicates.add(cb.lessThanOrEqualTo(variant.get("price"), max));
            predicates.add(cb.isTrue(variant.get("active")));
            predicates.add(cb.greaterThan(variant.get("stock"), 0));
            if (size != null && !size.isBlank())
              predicates.add(cb.equal(variant.get("size"), size));
            if (color != null && !color.isBlank())
              predicates.add(
                  cb.equal(cb.lower(variant.get("color")), color.toLowerCase(Locale.ROOT)));
            query.distinct(true);
          }
          return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    Sort order =
        switch (sort) {
          case "price-asc" -> Sort.by("price").ascending();
          case "price-desc" -> Sort.by("price").descending();
          case "name" -> Sort.by("name");
          case "newest" -> Sort.by("id").descending();
          default -> throw bad("Kiểu sắp xếp không hợp lệ");
        };
    Sort finalOrder = order.getOrderFor("id") == null ? order.and(Sort.by("id")) : order;
    var result = products.findAll(filter, PageRequest.of(page, limit, finalOrder));
    return new PageView<>(
        result.getContent().stream().map(CatalogService::view).toList(),
        page,
        result.getTotalPages(),
        result.getTotalElements());
  }

  public ProductView detail(Long id) {
    Product p = products.findById(id).orElseThrow(() -> missing("Không tìm thấy sản phẩm"));
    if (!p.isActive() || !p.getCategory().isActive()) throw missing("Sản phẩm đã ngừng bán");
    return view(p);
  }

  public List<CategoryView> categoryList() {
    return categories.findByActiveTrueOrderByNameAsc().stream()
        .map(c -> new CategoryView(c.getId(), c.getName()))
        .toList();
  }

  public Map<String, List<String>> facets() {
    var all =
        products.findAll().stream()
            .filter(p -> p.isActive() && p.getCategory().isActive())
            .toList();
    var variants =
        all.stream()
            .flatMap(p -> p.getVariants().stream())
            .filter(v -> v.isActive() && v.getStock() > 0)
            .toList();
    return Map.of(
        "brands",
        all.stream().map(Product::getBrand).distinct().sorted().toList(),
        "colors",
        variants.stream().map(ProductVariant::getColor).distinct().sorted().toList(),
        "sizes",
        variants.stream().map(ProductVariant::getSize).distinct().sorted().toList());
  }

  @Transactional
  public CategoryView createCategory(CategoryInput input) {
    Category c = new Category();
    c.setName(input.name().trim());
    categories.save(c);
    return new CategoryView(c.getId(), c.getName());
  }

  @Transactional
  public CategoryView updateCategory(Long id, CategoryInput input) {
    Category c = categories.findById(id).orElseThrow(() -> missing("Không tìm thấy danh mục"));
    c.setName(input.name().trim());
    return new CategoryView(c.getId(), c.getName());
  }

  @Transactional
  public void deleteCategory(Long id) {
    Category c = categories.findById(id).orElseThrow(() -> missing("Không tìm thấy danh mục"));
    if (products.existsByCategoryIdAndActiveTrue(id))
      throw bad("Hãy ngừng bán sản phẩm trước khi ẩn danh mục");
    c.setActive(false);
  }

  @Transactional
  public ProductView create(ProductInput input) {
    Product p = new Product();
    p.setCode("F6S-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
    apply(p, input);
    products.saveAndFlush(p);
    return view(p);
  }

  @Transactional
  public ProductView update(Long id, ProductInput input) {
    Product p = products.lockById(id).orElseThrow(() -> missing("Không tìm thấy sản phẩm"));
    apply(p, input);
    products.flush();
    return view(p);
  }

  @Transactional
  public void delete(Long id) {
    products.lockById(id).orElseThrow(() -> missing("Không tìm thấy sản phẩm")).setActive(false);
  }

  private void apply(Product p, ProductInput input) {
    Category category =
        categories
            .findById(input.categoryId())
            .filter(Category::isActive)
            .orElseThrow(() -> bad("Danh mục không hợp lệ"));
    p.setName(input.name().trim());
    p.setBrand(input.brand().trim());
    p.setPrice(input.price());
    p.setDescription(input.description().trim());
    p.setImageUrl(input.imageUrl());
    p.setHoverImageUrl(input.hoverImageUrl());
    p.setFeatured(input.featured());
    p.setCategory(category);
    var unique = new HashSet<String>();
    p.getVariants().forEach(v -> v.setActive(false));
    for (var row : input.variants()) {
      String color = row.color().trim();
      String key = color.toLowerCase(Locale.ROOT) + "/" + row.size();
      if (!unique.add(key)) throw bad("Trùng biến thể màu và size");
      ProductVariant v =
          p.getVariants().stream()
              .filter(x -> x.getColor().equalsIgnoreCase(color) && x.getSize().equals(row.size()))
              .findFirst()
              .orElse(null);
      if (v == null) {
        v = new ProductVariant();
        v.setProduct(p);
        v.setSku(
            "SKU-"
                + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 16)
                    .toUpperCase(Locale.ROOT));
        v.setColor(color);
        v.setSize(row.size());
        p.getVariants().add(v);
      }
      v.setStock(row.stock());
      v.setPrice(row.price() == null ? input.price() : row.price());
      v.setWeightGrams(row.weightGrams());
      v.setActive(true);
    }
    p.setPrice(
        p.getVariants().stream()
            .filter(ProductVariant::isActive)
            .map(ProductVariant::getPrice)
            .min(java.util.Comparator.naturalOrder())
            .orElseThrow());
    if (input.metadata() != null) {
      var metadata = input.metadata();
      p.setBrandAttribute(attributes.require(metadata.brandId(), "BRAND"));
      p.setSoleAttribute(attributes.require(metadata.soleId(), "SOLE"));
      p.setMaterialAttribute(attributes.require(metadata.materialId(), "MATERIAL"));
      if (p.getBrandAttribute() != null) p.setBrand(p.getBrandAttribute().getName());
      var images =
          metadata.images() == null
              ? java.util.List.<com.example.sneaker.dto.CatalogAdminApi.ColorImage>of()
              : metadata.images();
      var mapped = new java.util.ArrayList<ProductColorImage>();
      var order = new java.util.HashMap<String, Integer>();
      var seenImages = new java.util.HashSet<String>();
      for (var image : images) {
        String color = image.color().trim();
        if (!p.getColorways().contains(color)) throw bad("Ảnh phải gắn với màu có trong ma trận");
        var asset = media.require(image.mediaId().toString());
        if (!seenImages.add(color + "/" + asset.getId()))
          throw bad("Không gắn trùng một ảnh cho cùng màu");
        int position = order.merge(color, 1, Integer::sum) - 1;
        if (position >= 5) throw bad("Tối đa 5 ảnh cho mỗi màu");
        var link = new ProductColorImage();
        link.setProduct(p);
        link.setColor(color);
        link.setMedia(asset);
        link.setDisplayOrder(position);
        mapped.add(link);
      }
      p.getColorImages().clear();
      if (p.getId() != null) products.flush();
      p.getColorImages().addAll(mapped);
      if (!mapped.isEmpty()) {
        p.setImageUrl(mapped.get(0).getMedia().publicUrl());
        p.setHoverImageUrl(mapped.get(Math.min(1, mapped.size() - 1)).getMedia().publicUrl());
      }
    }
  }

  public static ProductView view(Product p) {
    return new ProductView(
        p.getId(),
        p.getCode(),
        p.getName(),
        p.getBrand(),
        p.getPrice(),
        p.getStock(),
        p.getDescription(),
        p.getImageUrl(),
        p.getHoverImageUrl(),
        p.isFeatured(),
        new CategoryView(p.getCategory().getId(), p.getCategory().getName()),
        p.getColorways(),
        p.getVariants().stream()
            .filter(ProductVariant::isActive)
            .map(
                v ->
                    new VariantView(
                        v.getId(),
                        v.getSku(),
                        v.getColor(),
                        v.getSize(),
                        v.getStock(),
                        v.getPrice(),
                        "F6S:VARIANT:" + v.getSku(),
                        v.getWeightGrams()))
            .toList(),
        new com.example.sneaker.dto.CatalogAdminApi.Details(
            ShoeAttributeService.view(p.getBrandAttribute()),
            ShoeAttributeService.view(p.getSoleAttribute()),
            ShoeAttributeService.view(p.getMaterialAttribute()),
            p.getColorImages().stream()
                .map(
                    i ->
                        new com.example.sneaker.dto.CatalogAdminApi.ImageView(
                            i.getColor(),
                            i.getMedia().getId(),
                            i.getMedia().publicUrl(),
                            i.getDisplayOrder()))
                .toList(),
            p.getCreatedAt(),
            p.getUpdatedAt(),
            isAdmin() ? p.getCreatedBy() : null,
            isAdmin() ? p.getUpdatedBy() : null));
  }

  private static boolean isAdmin() {
    var auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    return auth != null
        && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }

  public static ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  public static ResponseStatusException missing(String message) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
  }
}
