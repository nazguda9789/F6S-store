package com.example.demodatn.controller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import javax.sql.DataSource;
import java.sql.Connection;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class AuditSqlProbeTest {
 static final ThreadLocal<List<String>> QUERIES=new ThreadLocal<>();
 @Autowired WebApplicationContext context; @Autowired JdbcTemplate jdbc;
 @TestConfiguration static class Counter {
  @Bean static BeanPostProcessor auditCounter(){return new BeanPostProcessor(){public Object postProcessAfterInitialization(Object bean,String name){
   if(!(bean instanceof DataSource))return bean;
   return Proxy.newProxyInstance(DataSource.class.getClassLoader(),new Class[]{DataSource.class},(p,m,a)->{
    Object value=call(bean,m,a);if(value instanceof Connection c)return Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(cp,cm,ca)->{
     if(QUERIES.get()!=null&&cm.getName().equals("prepareStatement"))QUERIES.get().add((String)ca[0]);return call(c,cm,ca);
    });return value;
   });
  }};}
 }
 static Object call(Object o,Method m,Object[] a)throws Throwable{try{return m.invoke(o,a);}catch(InvocationTargetException e){throw e.getCause();}}
 @Test void measure()throws Exception{
  assertEquals("shopbangiay_kt_20261025",jdbc.queryForObject("SELECT DB_NAME()",String.class));
  assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM products",Integer.class)>=14);
  int product=jdbc.queryForObject("SELECT id FROM products WHERE code='QA-CAT-00'",Integer.class);
  int variant=jdbc.queryForObject("SELECT MIN(id) FROM product_variants WHERE product_id=?",Integer.class,product);
  var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  var session=new MockHttpSession();mvc.perform(post("/cart/add").session(session).with(user("admin").roles("ADMIN")).with(csrf()).param("variantId",""+variant).param("quantity","1"));
  Path out=Path.of("verify/full-audit/performance-"+System.getProperty("audit.stage","before"));Files.createDirectories(out);
  String[] paths={"/","/products","/products/"+product,"/category/1","/cart","/admin/pos","/admin/products-crud","/admin/inventory"};
  List<String> report=new ArrayList<>();
  for(int i=0;i<paths.length;i++){
   QUERIES.set(new ArrayList<>());var result=mvc.perform(get(paths[i]).session(session).with(user("admin").roles("ADMIN"))).andReturn();var queries=QUERIES.get();QUERIES.remove();
   assertEquals(200,result.getResponse().getStatus(),paths[i]);Files.writeString(out.resolve(i+".html"),result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));Files.write(out.resolve(i+".sql"),queries);report.add(paths[i]+" | "+queries.size());
  }
  Files.write(out.resolve("counts.txt"),report);System.out.println("AUDIT_SQL_COUNTS "+report);
 }
}
