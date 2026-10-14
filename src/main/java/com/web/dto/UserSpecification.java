package com.web.dto;

import com.web.entity.User;
import com.web.enums.Role;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
public class UserSpecification implements Specification<User> {

    private Role role;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String search;


    @Override
    public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate predicate = cb.conjunction();

        if (role != null) {
            predicate = cb.and(predicate, cb.equal(root.get("role"), role));
        }

        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern)
            );
            predicate = cb.and(predicate, searchPredicate);
        }

        if (fromDate != null && toDate != null) {
            Predicate datePredicate = cb.between(
                    root.get("createdDate").as(LocalDate.class),
                    fromDate,
                    toDate
            );
            predicate = cb.and(predicate, datePredicate);
        }

        return predicate;
    }
}
