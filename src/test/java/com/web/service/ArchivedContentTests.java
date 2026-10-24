package com.web.service;
import com.web.entity.Product;
import com.web.repository.BlogRepository;
import com.web.controller.user.ProductUserController;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ArchivedContentTests {
    @Test void publicBlogListUsesArchiveFilter(){var repo=mock(BlogRepository.class);var service=new BlogService();ReflectionTestUtils.setField(service,"blogRepository",repo);var page=PageRequest.of(0,10);when(repo.findByArchivedFalse(page)).thenReturn(Page.empty());assertTrue(service.findAllList(page).isEmpty());verify(repo,never()).findAll(page);}
    @Test void inactiveProductCannotOpenPublicDetail(){var products=mock(ProductService.class);var controller=new ProductUserController();ReflectionTestUtils.setField(controller,"productService",products);var p=new Product();p.setActive(false);when(products.findBySlug("retired")).thenReturn(p);var error=assertThrows(ResponseStatusException.class,()->controller.productView("retired",null,null,null));assertEquals(404,error.getStatusCode().value());}
    @Test void missingProductReturns404(){var products=mock(ProductService.class);var controller=new ProductUserController();ReflectionTestUtils.setField(controller,"productService",products);assertThrows(ResponseStatusException.class,()->controller.productView("missing",null,null,null));}
}
