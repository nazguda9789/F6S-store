package com.web.service;

import com.web.entity.Category;
import com.web.entity.Color;
import com.web.entity.Size;
import com.web.entity.ShoeAttribute;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Explicit create-only payload: never bind entity IDs or change existing rows here. */
@Service
public class CatalogQuickCreateService {
    public enum Group { CATEGORY, BRAND, SOLE, MATERIAL, COLOR, SIZE }
    public record Request(Group group, String name, String code, String slug, Long parentId, String description) {}
    public record Option(Long id, String name, String code, Group group) {}
    private final ShoeAttributeService attributes;
    private final CategoryService categories;
    private final ColorService colors;
    private final SizeService sizes;

    public CatalogQuickCreateService(ShoeAttributeService attributes, CategoryService categories, ColorService colors, SizeService sizes) {
        this.attributes=attributes; this.categories=categories; this.colors=colors; this.sizes=sizes;
    }

    @Transactional
    public Option create(Request request) {
        if(request==null || request.group()==null) throw new IllegalArgumentException("Chọn nhóm thuộc tính cần thêm.");
        return switch(request.group()) {
            case BRAND, SOLE, MATERIAL -> {
                var value=attributes.save(null,ShoeAttribute.Kind.valueOf(request.group().name()),request.name(),true,request.description());
                yield new Option(value.getId(),value.getName(),value.getCode(),request.group());
            }
            case COLOR -> {
                var color=new Color();color.setName(request.name());color.setCode(request.code());
                color=colors.saveOrUpdate(color);
                yield new Option(color.getId(),color.getName(),color.getCode(),request.group());
            }
            case SIZE -> {
                var size=new Size();size.setName(request.name());size=sizes.saveOrUpdate(size);
                yield new Option(size.getId(),size.getName(),null,request.group());
            }
            case CATEGORY -> {
                var category=new Category();category.setName(request.name());category.setSlug(request.slug());
                category.setIsParent(request.parentId()==null);category.setParentId(request.parentId());category.setPosition(0);
                category=categories.saveOrUpdate(category);
                yield new Option(category.getId(),category.getName(),null,request.group());
            }
        };
    }
}
