package com.example.sneaker;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.sneaker.dto.PeopleApi.*;
import com.example.sneaker.repository.StaffRepository;
import com.example.sneaker.service.MediaService;
import com.example.sneaker.service.PeopleService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:bee_modules_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.admin-password=test-password-long-enough",
      "app.seed-demo=false",
      "app.media-dir=/tmp/f6s-bee-test-media"
    })
@AutoConfigureMockMvc
class BeeModulesTest {
  @Autowired PeopleService people;
  @Autowired StaffRepository staff;
  @Autowired MediaService media;
  @Autowired MockMvc mvc;
  @Autowired com.example.sneaker.service.CatalogService catalog;
  @Autowired com.example.sneaker.service.ShoeAttributeService attributes;

  @Test
  void productImagesCanBeReplacedWithoutBreakingUniqueOrderAndCannotBeHiddenWhenUsed()
      throws Exception {
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB), "png", bytes);
    var image =
        media.upload(
            new MockMultipartFile("file", "shoe-test.png", "image/png", bytes.toByteArray()),
            "tester");
    var brand =
        attributes.create(
            new com.example.sneaker.dto.CatalogAdminApi.AttributeInput(
                "BRAND", "Test Brand " + UUID.randomUUID(), true));
    var category =
        catalog.createCategory(
            new com.example.sneaker.dto.Api.CategoryInput("Test " + UUID.randomUUID()));
    var metadata =
        new com.example.sneaker.dto.CatalogAdminApi.Metadata(
            brand.id(),
            null,
            null,
            List.of(
                new com.example.sneaker.dto.CatalogAdminApi.ColorImage(
                    "Đen", UUID.fromString(image.id()))));
    var input =
        new com.example.sneaker.dto.Api.ProductInput(
            "Giày thử",
            brand.name(),
            java.math.BigDecimal.valueOf(500000),
            "Mô tả",
            "/images/catalog-photo-pending.svg",
            "",
            category.id(),
            false,
            List.of(
                new com.example.sneaker.dto.Api.VariantInput(
                    "Đen", "40", 5, java.math.BigDecimal.valueOf(500000), 400)),
            metadata);
    var product = catalog.create(input);
    var updated = catalog.update(product.id(), input);
    assertThat(updated.metadata().images()).hasSize(1);
    assertThat(updated.variants().get(0).weightGrams()).isEqualTo(400);
    assertThat(updated.imageUrl()).isEqualTo(image.url());
    assertThat(updated.metadata().createdBy()).isNull();
    assertThatThrownBy(() -> media.disable(image.id())).isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                attributes.update(
                    brand.id(),
                    new com.example.sneaker.dto.CatalogAdminApi.AttributeInput(
                        "BRAND", "Renamed", true)))
        .isInstanceOf(ResponseStatusException.class);
  }

  Profile profile(String phone) {
    return new Profile("Khách kiểm thử", null, phone, "OTHER", null, true);
  }

  AddressInput address(boolean primary) {
    return new AddressInput(
        "Người nhận", "0912345678", "Hà Nội", "Ba Đình", "Kim Mã", "10 Phố mẫu", primary);
  }

  @Test
  void staffIsRestrictedAndDisablingImmediatelyRevokesAccess() throws Exception {
    String username = "staff" + UUID.randomUUID().toString().substring(0, 8),
        password = "test-staff-password-2026";
    var account =
        people.createStaff(
            new StaffCreate(
                username,
                password,
                new Profile(
                    "Nhân viên thử", username + "@example.test", "0987654321", "OTHER", null, true),
                ""));
    assertThat(staff.findByUsername(username).orElseThrow().getPasswordHash())
        .startsWith("{bcrypt}")
        .doesNotContain(password);
    String auth =
        "Basic "
            + Base64.getEncoder()
                .encodeToString(
                    (username + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    for (int i = 0; i < 2; i++)
      mvc.perform(get("/api/admin/session").header("Authorization", auth))
          .andExpect(status().isOk());
    mvc.perform(get("/api/admin/customers").header("Authorization", auth))
        .andExpect(status().isOk());
    mvc.perform(get("/api/admin/staff").header("Authorization", auth))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/products").header("Authorization", auth))
        .andExpect(status().isForbidden());
    people.disableStaff(account.id());
    mvc.perform(get("/api/admin/session").header("Authorization", auth))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/admin/customers")).andExpect(status().isUnauthorized());
  }

  @Test
  void addressesKeepExactlyOneDefaultAndRejectAnotherCustomersAddress() {
    var first = people.createCustomer(profile("0912345678"));
    var second = people.createCustomer(profile("0912345679"));
    var one = people.saveAddress(first.id(), null, address(false));
    assertThat(one.addresses()).hasSize(1);
    assertThat(one.addresses().get(0).defaultAddress()).isTrue();
    var two = people.saveAddress(first.id(), null, address(true));
    assertThat(two.addresses().stream().filter(AddressView::defaultAddress)).hasSize(1);
    Long primary = two.addresses().get(0).id();
    assertThatThrownBy(() -> people.deleteAddress(second.id(), primary))
        .isInstanceOf(ResponseStatusException.class);
    var after = people.deleteAddress(first.id(), primary);
    assertThat(after.addresses()).hasSize(1);
    assertThat(after.addresses().get(0).defaultAddress()).isTrue();
  }

  @Test
  void mediaVerifiesImageContentsAndDeduplicates() throws Exception {
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
    var image = new MockMultipartFile("file", "test.png", "image/png", bytes.toByteArray());
    var a = media.upload(image, "tester");
    var b = media.upload(image, "tester");
    assertThat(a.id()).isEqualTo(b.id());
    assertThatThrownBy(
            () ->
                media.upload(
                    new MockMultipartFile(
                        "file", "fake.png", "image/png", "not an image".getBytes()),
                    "tester"))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                media.upload(
                    new MockMultipartFile("file", "bad.svg", "image/svg+xml", "<svg/>".getBytes()),
                    "tester"))
        .isInstanceOf(ResponseStatusException.class);
  }
}
