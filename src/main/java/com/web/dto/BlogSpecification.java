package com.web.dto;

import com.web.entity.Blog;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Date;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
public class BlogSpecification implements Specification<Blog> {

    private Date from;
    private Date to;
    private String search;

    @Override
    public Predicate toPredicate(Root<Blog> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate predicate = cb.conjunction();

        // Tìm kiếm theo title hoặc description
        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
            predicate = cb.and(predicate, searchPredicate);
        }

        // Chuyển Date -> LocalDate để so sánh
        LocalDate fromDate = (from != null) ? from.toLocalDate() : null;
        LocalDate toDate   = (to != null) ? to.toLocalDate()   : null;

        if (fromDate != null && toDate != null) {
            Predicate datePredicate = cb.between(
                    root.get("createdDate").as(LocalDate.class),
                    fromDate,
                    toDate
            );
            predicate = cb.and(predicate, datePredicate);
        } else if (fromDate != null) {
            Predicate datePredicate = cb.greaterThanOrEqualTo(
                    root.get("createdDate").as(LocalDate.class),
                    fromDate
            );
            predicate = cb.and(predicate, datePredicate);
        } else if (toDate != null) {
            Predicate datePredicate = cb.lessThanOrEqualTo(
                    root.get("createdDate").as(LocalDate.class),
                    toDate
            );
            predicate = cb.and(predicate, datePredicate);
        }

        return predicate;
    }
}
