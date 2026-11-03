package com.example.demodatn;

import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.entity.sanpham.*;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.*;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.service.donhang.PosCatalogQuery;
import com.example.demodatn.service.nhanvien.AccountMailService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Portable regressions, not a substitute for SQL Server lock/schema tests. */
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:f6s_refresh;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "shop.schema-check.enabled=false"
})
class RefreshFlowTest {
    @Autowired WebApplicationContext context;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired CategoryRepository categories;
    @Autowired SizeRepository sizes;
    @Autowired MaterialRepository materials;
    @Autowired CustomerRepository customers;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired EmployeeRepository employees;
    @Autowired OrderRepository orders;
    // SQL Server-only metadata/range-lock queries are deliberately not emulated by H2.
    @MockitoBean AccountIssueRepository accountIssues;
    @MockitoBean PosCatalogQuery posQuery;
    @MockitoBean AccountMailService mail;
    MockMvc mvc;
    Product product;
    ProductVariant variant;
    Customer customer;

    @BeforeEach void setup() {
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        product=new Product();product.setName("Giày kiểm thử F6S");product.setCode("TEST-"+UUID.randomUUID());
        product.setCategory(categories.findAll().get(0));product.setImageUrl("/anh/anh-san-pham-thay-the.svg");
        product=products.saveAndFlush(product);
        variant=new ProductVariant();variant.setProduct(product);variant.setSize(sizes.findAll().get(0));
        variant.setMaterial(materials.findAll().get(0));variant.setQuantity(12);variant.setPrice(new BigDecimal("250000"));
        variant=variants.saveAndFlush(variant);
        User user=new User();user.setUsername("buyer"+UUID.randomUUID());user.setPassword("unused-test-hash");user.setRole(roles.findByName("ROLE_USER"));user=users.saveAndFlush(user);
        customer=new Customer();customer.setCustomerCode("KH"+System.nanoTime());customer.setFullName("Khách kiểm thử");
        customer.setUser(user);customer.setEmail(user.getUsername()+"@example.test");customer.setPhone(String.format("09%08d",user.getId()));customer.setAddress("123 Đường kiểm thử Hà Nội");
        customer=customers.saveAndFlush(customer);
        when(posQuery.products(anyString(),nullable(Integer.class),nullable(Integer.class),nullable(Integer.class),any()))
            .thenReturn(new PageImpl<>(List.of(product),PageRequest.of(0,12),1));
        when(posQuery.customers(anyString(),any())).thenReturn(Page.empty());
        when(mail.send(anyString(),anyString(),anyString(),anyString())).thenReturn(true);
    }

    MockHttpSession basket() {
        var session=new MockHttpSession();
        session.setAttribute("cart",new ArrayList<>(List.of(new CartItemDto(product.getId(),variant.getId(),product.getName(),variant.getSize().getSizeValue(),variant.getMaterial().getName(),variant.getPrice(),2))));
        return session;
    }
    String buyer() {return customer.getUser().getUsername();}
    void evidence(String name,String html) throws Exception {
        Path dir=Path.of("target/refresh-ui");Files.createDirectories(dir);Files.writeString(dir.resolve(name+".html"),html);
    }
    @Test void anonymousCheckoutRequiresLoginAndCustomerHeaderDiffers() throws Exception {
        mvc.perform(get("/checkout").session(basket())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        var anonymous=mvc.perform(get("/products/"+product.getId())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var signed=mvc.perform(get("/products/"+product.getId()).with(user(buyer()).roles("USER"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(anonymous.contains("Đăng nhập / Đăng ký"));assertFalse(anonymous.contains("Xin chào, "+buyer()));
        assertTrue(signed.contains("Xin chào, "+buyer()));assertFalse(signed.contains("id=\"quantity\" name=\"quantity\" value=\"1\" min=\"1\" max=\"1\" readonly"));
        evidence("product",signed);
    }
    @Test void codCheckoutPersistsOwnedOrderAndReplayDoesNotDuplicate() throws Exception {
        var session=basket();
        String html=mvc.perform(get("/checkout").session(session).with(user(buyer()).roles("USER")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        evidence("checkout",html);
        String token=(String)session.getAttribute("onlineCheckoutToken");assertNotNull(token);
        long count=orders.count();
        var request=post("/checkout/place").session(session).with(user(buyer()).roles("USER")).with(csrf())
            .param("checkoutToken",token).param("receiverName","Khách kiểm thử").param("phone","0912345678")
            .param("email","buyer@example.test").param("shippingAddress","123 Đường kiểm thử Hà Nội").param("paymentMethod","COD");
        String destination=mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertTrue(destination.startsWith("/orders/success/"));assertEquals(count+1,orders.count());
        mvc.perform(post("/checkout/place").session(session).with(user(buyer()).roles("USER")).with(csrf()).param("checkoutToken",token))
            .andExpect(redirectedUrl(destination));
        assertEquals(count+1,orders.count());
    }
    @Test void invalidReceiverKeepsCartAndPaymentMethodCannotBeForged() throws Exception {
        var session=basket();session.setAttribute("onlineCheckoutToken","test-token");long count=orders.count();
        mvc.perform(post("/checkout/place").session(session).with(user(buyer()).roles("USER")).with(csrf())
            .param("checkoutToken","test-token").param("receiverName","Khách").param("phone","abc")
            .param("email","invalid").param("shippingAddress","x").param("paymentMethod","PAID"))
            .andExpect(redirectedUrl("/checkout")).andExpect(flash().attributeExists("org.springframework.validation.BindingResult.checkoutRequest"));
        assertEquals(count,orders.count());assertNotNull(session.getAttribute("cart"));
    }
    @Test void newVariantCanCreateSizeAndMaterialAndDuplicateIsRejected() throws Exception {
        String newSize="S"+System.nanoTime()%100000, newMaterial="Chất liệu "+UUID.randomUUID();
        mvc.perform(post("/admin/variants/save").with(user("admin").roles("ADMIN")).with(csrf())
            .param("productId",product.getId().toString()).param("newSizeValue",newSize).param("newMaterialName",newMaterial)
            .param("price","300000").param("quantity","7").param("status","true"))
            .andExpect(status().is3xxRedirection());
        var all=variants.findByProduct_Id(product.getId());assertEquals(2,all.size());
        var created=all.stream().filter(v->v.getSize().getSizeValue().equals(newSize)).findFirst().orElseThrow();assertEquals(7,created.getQuantity());
        mvc.perform(post("/admin/variants/save").with(user("admin").roles("ADMIN")).with(csrf())
            .param("productId",product.getId().toString()).param("sizeId",created.getSize().getId().toString())
            .param("materialId",created.getMaterial().getId().toString()).param("price","300000").param("quantity","7").param("status","true"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("variant","materialId"));
    }
    @Test void staffCreationGeneratesAccountAndEmailsWithoutExposingPasswordInResponse() throws Exception {
        String key="employee"+System.nanoTime(),email=key+"@example.test";
        var response=mvc.perform(post("/admin/employees/save").with(user("admin").roles("ADMIN")).with(csrf())
            .param("employeeCode","NV"+System.nanoTime()).param("fullName","Nhân viên kiểm thử")
            .param("phone","0987654321").param("email",email).param("username",key).param("position","Thu ngân"))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse();
        assertTrue(response.getRedirectedUrl().startsWith("/admin/employees/detail/"));
        var saved=users.findByUsername(key).orElseThrow();assertTrue(saved.getMustChangePassword());assertEquals(email,saved.getEmail());
        verify(mail).send(eq(email),eq("Nhân viên kiểm thử"),eq(key),anyString());
        assertFalse(response.getContentAsString().contains(saved.getPassword()));
    }
    @Test void posShowsWorkflowStockAndVariantCount() throws Exception {
        var html=mvc.perform(get("/admin/pos").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("Kiểm tra sản phẩm trong giỏ"));assertTrue(html.contains("Tồn: 12 đôi"));
        assertTrue(html.contains("data-variant-count=\"1\""));assertTrue(html.contains("Bán hàng online"));
        evidence("pos",html);
    }

    @Test void transferStaysUnpaidUntilAdminConfirmationAndReservesOnlyOnce() throws Exception {
        var session=basket();session.setAttribute("onlineCheckoutToken","transfer-token");
        var response=mvc.perform(post("/checkout/place").session(session).with(user(buyer()).roles("USER")).with(csrf())
            .param("checkoutToken","transfer-token").param("receiverName","Khách chuyển khoản").param("phone","0912345678")
            .param("email","buyer@example.test").param("shippingAddress","123 Đường kiểm thử Hà Nội").param("paymentMethod","BANK_TRANSFER"))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse();
        String location=response.getRedirectedUrl();assertTrue(location.startsWith("/orders/success/"));
        Integer id=Integer.valueOf(location.substring(location.lastIndexOf('/')+1));
        assertEquals("UNPAID",orders.findById(id).orElseThrow().getPaymentStatus());
        assertEquals(10,variants.findById(variant.getId()).orElseThrow().getQuantity());
        mvc.perform(post("/admin/orders/"+id+"/confirm-payment").with(user("admin").roles("ADMIN")).with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertEquals("PAID",orders.findById(id).orElseThrow().getPaymentStatus());
        assertEquals(10,variants.findById(variant.getId()).orElseThrow().getQuantity());
    }

    @Test void posCreatesPaidInvoiceAfterCashAndDeductsStockOnce() throws Exception {
        var session=new MockHttpSession();long before=orders.count();
        mvc.perform(post("/admin/pos/add").session(session).with(user("admin").roles("ADMIN")).with(csrf())
            .param("variantId",variant.getId().toString()).param("quantity","2"))
            .andExpect(status().is3xxRedirection());
        assertEquals(before,orders.count());assertEquals(12,variants.findById(variant.getId()).orElseThrow().getQuantity());
        mvc.perform(get("/admin/pos").session(session).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        String token=(String)session.getAttribute("posCheckoutToken");
        var response=mvc.perform(post("/admin/pos/checkout").session(session).with(user("admin").roles("ADMIN")).with(csrf())
            .param("checkoutToken",token).param("paymentMethod","CASH").param("cashReceived","600000"))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse();
        String location=response.getRedirectedUrl();assertTrue(location.startsWith("/admin/pos/success/"));
        Integer id=Integer.valueOf(location.substring(location.lastIndexOf('/')+1));
        assertEquals("PAID",orders.findById(id).orElseThrow().getPaymentStatus());
        assertEquals(10,variants.findById(variant.getId()).orElseThrow().getQuantity());
        mvc.perform(get(location).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(post("/admin/pos/checkout").session(session).with(user("admin").roles("ADMIN")).with(csrf())
            .param("checkoutToken",token).param("paymentMethod","CASH").param("cashReceived","600000"))
            .andExpect(status().is3xxRedirection());
        assertEquals(before+1,orders.count());
    }
}
