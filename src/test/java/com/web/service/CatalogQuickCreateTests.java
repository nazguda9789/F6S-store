package com.web.service;

import com.web.entity.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogQuickCreateTests {
    private final ShoeAttributeService attributes=mock(ShoeAttributeService.class);
    private final CategoryService categories=mock(CategoryService.class);
    private final ColorService colors=mock(ColorService.class);
    private final SizeService sizes=mock(SizeService.class);
    private final CatalogQuickCreateService service=new CatalogQuickCreateService(attributes,categories,colors,sizes);
    private CatalogQuickCreateService.Request request(CatalogQuickCreateService.Group group){return new CatalogQuickCreateService.Request(group,"Giày mới","#123456","giay-moi",null,"Mô tả");}

    @Test void rejectsMissingGroup(){assertThrows(IllegalArgumentException.class,()->service.create(null));assertThrows(IllegalArgumentException.class,()->service.create(request(null)));verifyNoInteractions(attributes,categories,colors,sizes);}
    @Test void usesExistingAttributeValidationAndServerCodes(){
        for(var group:java.util.List.of(CatalogQuickCreateService.Group.BRAND,CatalogQuickCreateService.Group.SOLE,CatalogQuickCreateService.Group.MATERIAL)){
            var a=new ShoeAttribute();a.setId(7L);a.setName("Giày mới");a.setCode("SERVER-CODE");
            when(attributes.save(null,ShoeAttribute.Kind.valueOf(group.name()),"Giày mới",true,"Mô tả")).thenReturn(a);
            var result=service.create(request(group));assertEquals(7L,result.id());assertEquals("SERVER-CODE",result.code());assertEquals(group,result.group());
        }
    }
    @Test void createsRootCategoryWithoutAcceptingClientIds(){
        when(categories.saveOrUpdate(any())).thenAnswer(inv->{Category c=inv.getArgument(0);assertNull(c.getId());assertTrue(c.getIsParent());assertEquals("giay-moi",c.getSlug());c.setId(6L);return c;});
        assertEquals(6L,service.create(request(CatalogQuickCreateService.Group.CATEGORY)).id());
    }
    @Test void delegatesChildParentValidation(){
        when(categories.saveOrUpdate(any())).thenAnswer(inv->{Category c=inv.getArgument(0);assertFalse(c.getIsParent());assertEquals(3L,c.getParentId());c.setId(6L);return c;});
        var result=service.create(new CatalogQuickCreateService.Request(CatalogQuickCreateService.Group.CATEGORY,"Giày","","giay",3L,null));assertEquals(6L,result.id());
    }
    @Test void createsColorThroughHexValidator(){
        when(colors.saveOrUpdate(any())).thenAnswer(inv->{Color c=inv.getArgument(0);assertNull(c.getId());assertEquals("#123456",c.getCode());c.setId(8L);return c;});
        assertEquals(8L,service.create(request(CatalogQuickCreateService.Group.COLOR)).id());
    }
    @Test void createsSizeWithoutChangingExistingRows(){
        when(sizes.saveOrUpdate(any())).thenAnswer(inv->{Size s=inv.getArgument(0);assertNull(s.getId());s.setId(9L);return s;});
        assertEquals(9L,service.create(request(CatalogQuickCreateService.Group.SIZE)).id());
    }
    @Test void propagatesValidationFailure(){when(sizes.saveOrUpdate(any())).thenThrow(new IllegalArgumentException("invalid"));assertThrows(IllegalArgumentException.class,()->service.create(request(CatalogQuickCreateService.Group.SIZE)));}
}
