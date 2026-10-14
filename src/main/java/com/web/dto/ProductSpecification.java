package com.web.dto;

import com.web.entity.Product;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecification implements Specification<Product> {

    private Long categoryId;
    private String search;
    private Boolean active;
    private Double smallPrice;
    private Double largePrice;


    @Override
    public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate predicate = cb.conjunction();

        if (categoryId != null) {
            Join<Object, Object> joinProductCategory = root.join("productCategories", JoinType.INNER);
            Join<Object, Object> joinCategory = joinProductCategory.join("category", JoinType.INNER);
            predicate = cb.and(predicate, cb.equal(joinCategory.get("id"), categoryId));
        }

        if (active != null) {
            predicate = cb.and(predicate, cb.equal(root.get("active"), active));
        }

        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";

            Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
            predicate = cb.and(predicate, searchPredicate);
        }
        if (smallPrice != null) {
            predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("price"), smallPrice));
        }

        if (largePrice != null) {
            predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("price"), largePrice));
        }

        return predicate;
    }
}
