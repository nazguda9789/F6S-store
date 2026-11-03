package com.example.demodatn.controller.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@WithMockUser(username="user",roles="USER")
class UserUiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    private Integer fixture(String username) {
        String tag=java.util.UUID.randomUUID().toString().substring(0,8);
        int role=jdbc.queryForObject("SELECT id FROM roles WHERE name='ROLE_USER'",Integer.class);
        int userId=jdbc.queryForObject("INSERT users(username,password,role_id,status) OUTPUT INSERTED.id VALUES(?,'not-used-by-mock-login',?,1)",Integer.class,username,role);
        int customer=jdbc.queryForObject("INSERT customers(customer_code,full_name,user_id,status) OUTPUT INSERTED.id VALUES(?,?,?,1)",Integer.class,"UI-"+tag,"UI fixture",userId);
        return jdbc.queryForObject("INSERT orders(order_code,customer_id,order_type,status,payment_status,total_amount,final_amount,created_at) OUTPUT INSERTED.id VALUES(?,?,'ONLINE','PENDING','UNPAID',0,0,GETDATE())",Integer.class,"UI-"+tag,customer);
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void historyAndOwnedDetailUseSharedLayoutAndResponsiveTables() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String username="ui-own-"+java.util.UUID.randomUUID();
        Integer id=fixture(username);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(username,"",java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));
        mvc.perform(get("/orders")).andExpect(status().isOk()).andExpect(content().string(containsString("/css/he-thong-thiet-ke.css"))).andExpect(content().string(containsString("table-responsive")));
        mvc.perform(get("/orders/"+id)).andExpect(status().isOk()).andExpect(content().string(containsString("table-responsive"))).andExpect(content().string(not(containsString("customerName"))));
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void orderDetailAndSuccessRejectOtherCustomersAndMissingIds() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        Integer id=fixture("ui-other-"+java.util.UUID.randomUUID());
        mvc.perform(get("/orders/"+id)).andExpect(status().isForbidden());
        mvc.perform(get("/orders/success/"+id)).andExpect(status().isForbidden());
        mvc.perform(get("/orders/-1")).andExpect(status().isNotFound());
        mvc.perform(get("/orders/success/-1")).andExpect(status().isNotFound());
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void catalogDoesNotRenderLinksToProductsWithoutActiveStatus() throws Exception {
        jdbc.update("UPDATE products SET status=NULL WHERE id=4");
        MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build().perform(get("/"))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("href=\"/products/4\""))));
    }
    @Test
    void cartLoadsItsExistingActionsInsideLayoutContent() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build().perform(get("/cart"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/js/gio-hang.js"))).andExpect(content().string(containsString("store-confirm")));
    }
}
