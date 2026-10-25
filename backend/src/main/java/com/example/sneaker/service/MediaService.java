package com.example.sneaker.service;

import static com.example.sneaker.service.CatalogService.*;

import com.example.sneaker.dto.Api.PageView;
import com.example.sneaker.dto.CatalogAdminApi.MediaView;
import com.example.sneaker.model.MediaAsset;
import com.example.sneaker.repository.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class MediaService {
  private final MediaRepository media;
  private final ProductColorImageRepository colorImages;
  private final ProductRepository products;
  private final Path root;

  public MediaService(
      MediaRepository media,
      ProductColorImageRepository colorImages,
      ProductRepository products,
      @Value("${app.media-dir:./uploads/media}") String directory) {
    this.media = media;
    this.colorImages = colorImages;
    this.products = products;
    this.root = Path.of(directory).toAbsolutePath().normalize();
  }

  public PageView<MediaView> list(String q, int page) {
    var result =
        media.findAll(
            (r, query, cb) -> {
              var active = cb.isTrue(r.get("active"));
              if (q == null || q.isBlank()) return active;
              String value =
                  "%"
                      + q.trim()
                          .toLowerCase(Locale.ROOT)
                          .replace("\\", "\\\\")
                          .replace("%", "\\%")
                          .replace("_", "\\_")
                          .replace("[", "\\[")
                      + "%";
              return cb.and(active, cb.like(cb.lower(r.get("originalName")), value, '\\'));
            },
            PageRequest.of(page, 24, Sort.by("createdAt").descending()));
    return new PageView<>(
        result.getContent().stream().map(MediaService::view).toList(),
        page,
        result.getTotalPages(),
        result.getTotalElements());
  }

  @Transactional
  public MediaView upload(MultipartFile file, String actor) {
    if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
      throw bad("Ảnh phải có dữ liệu và tối đa 5 MB");
    try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) throw bad("Chỉ chấp nhận ảnh JPEG/PNG hợp lệ");
      var reader = readers.next();
      byte[] normalized;
      String extension;
      int width, height;
      try {
        reader.setInput(input, true, true);
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!Set.of("png", "jpeg", "jpg").contains(format))
          throw bad("Chỉ hỗ trợ PNG hoặc JPEG, không hỗ trợ SVG/GIF");
        width = reader.getWidth(0);
        height = reader.getHeight(0);
        if (width < 1
            || height < 1
            || width > 6000
            || height > 6000
            || (long) width * height > 12000000)
          throw bad("Ảnh quá lớn: tối đa 12 triệu pixel và 6000 px/cạnh");
        var image = reader.read(0);
        extension = format.equals("png") ? "png" : "jpg";
        var bytes = new ByteArrayOutputStream();
        if (!ImageIO.write(image, extension, bytes)) throw bad("Không mã hóa được ảnh");
        normalized = bytes.toByteArray();
      } finally {
        reader.dispose();
      }
      String hash =
          HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized));
      var old = media.findBySha256(hash);
      if (old.isPresent()) {
        old.get().setActive(true);
        return view(old.get());
      }
      Files.createDirectories(root);
      String storedName = hash + "." + extension;
      Path destination = root.resolve(storedName);
      Path temporary = Files.createTempFile(root, "upload-", ".tmp");
      try {
        Files.write(temporary, normalized);
        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
      } finally {
        Files.deleteIfExists(temporary);
      }
      var asset = new MediaAsset();
      asset.setId(UUID.randomUUID().toString());
      asset.setSha256(hash);
      asset.setStoredName(storedName);
      String name =
          Objects.toString(file.getOriginalFilename(), "image")
              .replaceAll("[^\\p{L}\\p{N}._ -]", "_");
      asset.setOriginalName(name.substring(0, Math.min(120, name.length())));
      asset.setContentType(extension.equals("png") ? "image/png" : "image/jpeg");
      asset.setWidth(width);
      asset.setHeight(height);
      asset.setByteSize(normalized.length);
      asset.setUploadedBy(actor);
      media.saveAndFlush(asset);
      return view(asset);
    } catch (org.springframework.web.server.ResponseStatusException e) {
      throw e;
    } catch (IOException e) {
      throw bad("Không đọc/lưu được ảnh. Kiểm tra định dạng và quyền ghi MEDIA_DIR.");
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public MediaAsset require(String id) {
    return media
        .findById(id)
        .filter(MediaAsset::isActive)
        .orElseThrow(() -> missing("Không tìm thấy ảnh đang hoạt động"));
  }

  public Resource resource(MediaAsset asset) {
    Path path = root.resolve(asset.getStoredName()).normalize();
    if (!path.startsWith(root) || !Files.isRegularFile(path))
      throw missing("File ảnh không còn trên máy chủ");
    return new FileSystemResource(path);
  }

  @Transactional
  public void disable(String id) {
    var asset = require(id);
    if (colorImages.existsByMediaId(id)
        || products.existsByImageUrlOrHoverImageUrl(asset.publicUrl(), asset.publicUrl()))
      throw bad("Ảnh đang được sản phẩm sử dụng; gỡ liên kết trước khi ẩn");
    asset.setActive(false);
  }

  public static MediaView view(MediaAsset m) {
    return new MediaView(
        m.getId(),
        m.publicUrl(),
        m.getOriginalName(),
        m.getWidth(),
        m.getHeight(),
        m.getByteSize(),
        m.getUploadedBy(),
        m.getCreatedAt());
  }
}
