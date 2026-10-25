package com.example.sneaker.controller;

import com.example.sneaker.dto.Api.PageView;
import com.example.sneaker.dto.CatalogAdminApi.*;
import com.example.sneaker.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Validated
public class CatalogToolsController {
  private final ShoeAttributeService attributes;
  private final MediaService media;

  @GetMapping("/api/admin/attributes")
  public List<AttributeView> attributes(
      @RequestParam String type, @RequestParam(defaultValue = "false") boolean includeInactive) {
    return attributes.list(type, includeInactive);
  }

  @PostMapping("/api/admin/attributes")
  @ResponseStatus(HttpStatus.CREATED)
  public AttributeView attribute(@Valid @RequestBody AttributeInput input) {
    return attributes.create(input);
  }

  @PutMapping("/api/admin/attributes/{id}")
  public AttributeView attribute(@PathVariable Long id, @Valid @RequestBody AttributeInput input) {
    return attributes.update(id, input);
  }

  @GetMapping("/api/admin/media")
  public PageView<MediaView> media(
      @RequestParam(required = false) @Size(max = 120) String q,
      @RequestParam(defaultValue = "0") @Min(0) int page) {
    return media.list(q, page);
  }

  @PostMapping(value = "/api/admin/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public MediaView media(@RequestParam("file") MultipartFile file, Principal user) {
    return media.upload(file, user.getName());
  }

  @DeleteMapping("/api/admin/media/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void media(@PathVariable UUID id) {
    media.disable(id.toString());
  }

  @GetMapping("/api/media/{id}")
  public ResponseEntity<Resource> image(@PathVariable UUID id) {
    var asset = media.require(id.toString());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(asset.getContentType()))
        .header("X-Content-Type-Options", "nosniff")
        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
        .body(media.resource(asset));
  }
}
