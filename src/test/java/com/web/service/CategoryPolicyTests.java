package com.web.service;
import com.web.entity.Category;
import com.web.exception.MessageException;
import com.web.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CategoryPolicyTests {
    @Test void rejectsBlankNameBeforeDatabaseWrite(){assertThrows(MessageException.class,()->new CategoryService().saveOrUpdate(new Category()));}
    @Test void preventsSelfParent(){
        var repo=mock(CategoryRepository.class);var service=new CategoryService();ReflectionTestUtils.setField(service,"categoryRepository",repo);
        var c=new Category();c.setId(5L);c.setParentId(5L);c.setName("Giày");c.setSlug("giay");
        var parent=new Category();parent.setId(5L);parent.setIsParent(true);when(repo.findById(5L)).thenReturn(Optional.of(parent));
        assertThrows(MessageException.class,()->service.saveOrUpdate(c));verify(repo,never()).save(any());
    }
    @Test void preventsCascadingCategoryDeletion(){
        var repo=mock(CategoryRepository.class);var service=new CategoryService();ReflectionTestUtils.setField(service,"categoryRepository",repo);
        var c=new Category();c.setChildren(List.of(new Category()));when(repo.findById(5L)).thenReturn(Optional.of(c));
        assertThrows(MessageException.class,()->service.delete(5L));verify(repo,never()).deleteById(any());
    }
}
